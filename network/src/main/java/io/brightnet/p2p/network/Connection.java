package io.brightnet.p2p.network;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import io.brightnet.app.Log;
import io.brightnet.app.Version;
import io.brightnet.common.ByteArrayUtils;
import io.brightnet.common.UserThread;
import io.brightnet.p2p.Message;
import io.brightnet.p2p.NodeAddress;
import io.brightnet.p2p.Utils;
import io.brightnet.p2p.network.messages.CloseConnectionMessage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Connection is created by the server thread or by sendMessage from NetworkNode.
 * All handlers are called on User thread.
 * Shared data between InputHandler thread and that
 */
public class Connection implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(Connection.class);
    private static final int MAX_MSG_SIZE = 5 * 1024 * 1024;         // 5 MB of compressed data
    //timeout on blocking Socket operations like ServerSocket.accept() or SocketInputStream.read()
    private static final int SOCKET_TIMEOUT = 10 * 60 * 1000;        // 10 min.
    private ConnectionPriority connectionPriority;

    public static int getMaxMsgSize() {
        return MAX_MSG_SIZE;
    }

    private final Socket socket;
    private final MessageListener messageListener;
    private final ConnectionListener connectionListener;

    private final String portInfo;
    private final String uid = UUID.randomUUID().toString();
    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    // holder of state shared between InputHandler and Connection
    private final SharedSpace sharedSpace;

    // set in init
    private InputHandler inputHandler;
    private ObjectOutputStream objectOutputStream;

    // mutable data, set from other threads but not changed internally.
    private Optional<NodeAddress> peerAddressOptional = Optional.empty();
    private volatile boolean isAuthenticated;
    private volatile boolean stopped;

    //TODO got java.util.zip.DataFormatException: invalid distance too far back
    // java.util.zip.DataFormatException: invalid literal/lengths set
    // use GZIPInputStream but problems with blocking
    private final boolean useCompression = false;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    public Connection(Socket socket, MessageListener messageListener, ConnectionListener connectionListener) {
        this.socket = socket;
        this.messageListener = messageListener;
        this.connectionListener = connectionListener;

        sharedSpace = new SharedSpace(this, socket);

        Log.traceCall();
        if (socket.getLocalPort() == 0)
            portInfo = "port=" + socket.getPort();
        else
            portInfo = "localPort=" + socket.getLocalPort() + "/port=" + socket.getPort();

        init();
    }

    private void init() {
        Log.traceCall();

        try {
            socket.setSoTimeout(SOCKET_TIMEOUT);
            // Need to access first the ObjectOutputStream otherwise the ObjectInputStream would block
            // See: https://stackoverflow.com/questions/5658089/java-creating-a-new-objectinputstream-blocks/5658109#5658109
            // When you construct an ObjectInputStream, in the constructor the class attempts to read a header that 
            // the associated ObjectOutputStream on the other end of the connection has written.
            // It will not return until that header has been read. 
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());


            // We create a thread for handling inputStream data
            inputHandler = new InputHandler(sharedSpace, objectInputStream, portInfo, this, useCompression);
            singleThreadExecutor.submit(inputHandler);
        } catch (IOException e) {
            sharedSpace.handleConnectionException(e);
        }

        sharedSpace.updateLastActivityDate();

        log.trace("\nNew connection created " + this.toString());
        UserThread.execute(() -> connectionListener.onConnection(this));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    // Called form UserThread
    public void setAuthenticated() {
        Log.traceCall();
        isAuthenticated = true;
    }

    public void setConnectionPriority(ConnectionPriority connectionPriority) {
        this.connectionPriority = connectionPriority;
    }

    // Called form various threads
    public void sendMessage(Message message) {
        Log.traceCall();
        if (!stopped) {
            try {
                String peerAddress = peerAddressOptional.isPresent() ? peerAddressOptional.get().toString() : "null";
                log.info("\n\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n" +
                        "Write object to outputStream to peer: {} (uid={})\nmessage={}"
                        + "\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n", peerAddress, uid, message);

                Object objectToWrite;
                if (useCompression) {
                    byte[] messageAsBytes = ByteArrayUtils.objectToByteArray(message);
                    // log.trace("Write object uncompressed data size: " + messageAsBytes.length);
                    byte[] compressed = Utils.compress(message);
                    //log.trace("Write object compressed data size: " + compressed.length);
                    objectToWrite = compressed;
                } else {
                    // log.trace("Write object data size: " + ByteArrayUtils.objectToByteArray(message).length);
                    objectToWrite = message;
                }
                if (!stopped) {
                    synchronized (objectOutputStream) {
                        objectOutputStream.writeObject(objectToWrite);
                        objectOutputStream.flush();
                    }
                    sharedSpace.updateLastActivityDate();
                }
            } catch (IOException e) {
                // an exception lead to a shutdown
                sharedSpace.handleConnectionException(e);
            }
        } else {
            log.debug("called sendMessage but was already stopped");
        }
    }

    public void reportIllegalRequest(IllegalRequest illegalRequest) {
        Log.traceCall();
        sharedSpace.reportIllegalRequest(illegalRequest);
    }

    public synchronized void setPeerAddress(NodeAddress peerNodeAddress) {
        Log.traceCall();
        checkNotNull(peerNodeAddress, "peerAddress must not be null");
        peerAddressOptional = Optional.of(peerNodeAddress);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener implementation
    ///////////////////////////////////////////////////////////////////////////////////////////

    // Only get non - CloseConnectionMessage messages
    @Override
    public void onMessage(Message message, Connection connection) {
        // connection is null as we get called from InputHandler, which does not hold a reference to Connection
        UserThread.execute(() -> messageListener.onMessage(message, this));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Getters
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Nullable
    public synchronized NodeAddress getPeerAddress() {
        return peerAddressOptional.isPresent() ? peerAddressOptional.get() : null;
    }

    public synchronized Optional<NodeAddress> getPeerAddressOptional() {
        return peerAddressOptional;
    }

    public Date getLastActivityDate() {
        return sharedSpace.getLastActivityDate();
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public String getUid() {
        return uid;
    }

    public boolean isStopped() {
        return stopped;
    }

    public ConnectionPriority getConnectionPriority() {
        return connectionPriority;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // ShutDown
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void shutDown(Runnable completeHandler) {
        shutDown(true, completeHandler);
    }

    public void shutDown() {
        shutDown(true, null);
    }

    private void shutDown(boolean sendCloseConnectionMessage) {
        shutDown(sendCloseConnectionMessage, null);
    }

    private void shutDown(boolean sendCloseConnectionMessage, @Nullable Runnable shutDownCompleteHandler) {
        Log.traceCall(this.toString());
        if (!stopped) {
            String peerAddress = peerAddressOptional.isPresent() ? peerAddressOptional.get().toString() : "null";
            log.info("\n\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n" +
                    "ShutDown connection:"
                    + "\npeerAddress=" + peerAddress
                    + "\nlocalPort/port=" + sharedSpace.getSocket().getLocalPort()
                    + "/" + sharedSpace.getSocket().getPort()
                    + "\nuid=" + uid
                    + "\nisAuthenticated=" + isAuthenticated
                    + "\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");

            log.trace("ShutDown connection requested. Connection=" + this.toString());

            if (sendCloseConnectionMessage) {
                new Thread(() -> {
                    Thread.currentThread().setName("Connection:SendCloseConnectionMessage-" + this.uid);
                    Log.traceCall("sendCloseConnectionMessage");
                    try {
                        sendMessage(new CloseConnectionMessage());
                        setStopFlags();

                        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                    } catch (Throwable t) {
                        log.error(t.getMessage());
                        t.printStackTrace();
                    } finally {
                        UserThread.execute(() -> doShutDown(shutDownCompleteHandler));
                    }
                }).start();
            } else {
                setStopFlags();
                doShutDown(shutDownCompleteHandler);
            }
        }
    }

    private void setStopFlags() {
        stopped = true;
        sharedSpace.stop();
        if (inputHandler != null)
            inputHandler.stop();
        isAuthenticated = false;
    }

    private void doShutDown(@Nullable Runnable shutDownCompleteHandler) {
        Log.traceCall();
        ConnectionListener.Reason shutDownReason = sharedSpace.getShutDownReason();
        if (shutDownReason == null)
            shutDownReason = ConnectionListener.Reason.SHUT_DOWN;
        final ConnectionListener.Reason finalShutDownReason = shutDownReason;
        // keep UserThread.execute as its not clear if that is called from a non-UserThread
        UserThread.execute(() -> connectionListener.onDisconnect(finalShutDownReason, this));

        try {
            sharedSpace.getSocket().close();
        } catch (SocketException e) {
            log.trace("SocketException at shutdown might be expected " + e.getMessage());
        } catch (IOException e) {
            log.error("Exception at shutdown. " + e.getMessage());
            e.printStackTrace();
        } finally {
            MoreExecutors.shutdownAndAwaitTermination(singleThreadExecutor, 500, TimeUnit.MILLISECONDS);

            log.debug("Connection shutdown complete " + this.toString());
            // keep UserThread.execute as its not clear if that is called from a non-UserThread

            if (shutDownCompleteHandler != null)
                UserThread.execute(shutDownCompleteHandler);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Connection)) return false;

        Connection that = (Connection) o;

        if (portInfo != null ? !portInfo.equals(that.portInfo) : that.portInfo != null) return false;
        if (uid != null ? !uid.equals(that.uid) : that.uid != null) return false;
        return peerAddressOptional != null ? peerAddressOptional.equals(that.peerAddressOptional) : that.peerAddressOptional == null;

    }

    @Override
    public int hashCode() {
        int result = portInfo != null ? portInfo.hashCode() : 0;
        result = 31 * result + (uid != null ? uid.hashCode() : 0);
        result = 31 * result + (peerAddressOptional != null ? peerAddressOptional.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Connection{" +
                "portInfo=" + portInfo +
                ", uid='" + uid + '\'' +
                ", sharedSpace=" + sharedSpace.toString() +
                ", peerAddress=" + peerAddressOptional +
                ", isAuthenticated=" + isAuthenticated +
                ", stopped=" + stopped +
                ", stopped=" + stopped +
                ", connectionType=" + connectionPriority +
                ", useCompression=" + useCompression +
                '}';
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // SharedSpace
    ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Holds all shared data between Connection and InputHandler
     * Runs in same thread as Connection
     */
    private static class SharedSpace {
        private static final Logger log = LoggerFactory.getLogger(SharedSpace.class);

        private final Connection connection;
        private final Socket socket;
        private final ConcurrentHashMap<IllegalRequest, Integer> illegalRequests = new ConcurrentHashMap<>();

        // mutable
        private Date lastActivityDate;
        private volatile boolean stopped;
        private ConnectionListener.Reason shutDownReason;

        public SharedSpace(Connection connection, Socket socket) {
            Log.traceCall();
            this.connection = connection;
            this.socket = socket;
        }

        public synchronized void updateLastActivityDate() {
            Log.traceCall();
            lastActivityDate = new Date();
        }

        public synchronized Date getLastActivityDate() {
            // Log.traceCall();
            return lastActivityDate;
        }

        public void reportIllegalRequest(IllegalRequest illegalRequest) {
            Log.traceCall();
            log.warn("We got reported an illegal request " + illegalRequest);
            log.debug("connection={}" + this);
            int violations;
            if (illegalRequests.contains(illegalRequest))
                violations = illegalRequests.get(illegalRequest);
            else
                violations = 0;

            violations++;
            illegalRequests.put(illegalRequest, violations);

            if (violations >= illegalRequest.maxTolerance) {
                log.warn("We close connection as we received too many invalid requests.\n" +
                        "violations={}\n" +
                        "illegalRequest={}\n" +
                        "illegalRequests={}", violations, illegalRequest, illegalRequests.toString());
                log.debug("connection={}" + this);
                shutDown(false);
            } else {
                illegalRequests.put(illegalRequest, ++violations);
            }
        }

        public void handleConnectionException(Throwable e) {
            Log.traceCall(e.toString());
            if (e instanceof SocketException) {
                if (socket.isClosed())
                    shutDownReason = ConnectionListener.Reason.SOCKET_CLOSED;
                else
                    shutDownReason = ConnectionListener.Reason.RESET;
            } else if (e instanceof SocketTimeoutException || e instanceof TimeoutException) {
                shutDownReason = ConnectionListener.Reason.TIMEOUT;
                log.warn("TimeoutException at socket " + socket.toString());
                log.debug("connection={}" + this);
            } else if (e instanceof EOFException) {
                shutDownReason = ConnectionListener.Reason.PEER_DISCONNECTED;
            } else if (e instanceof NoClassDefFoundError || e instanceof ClassNotFoundException) {
                shutDownReason = ConnectionListener.Reason.INCOMPATIBLE_DATA;
            } else {
                shutDownReason = ConnectionListener.Reason.UNKNOWN;
                log.warn("Exception at socket " + socket.toString());
                log.debug("connection={}" + this);
                e.printStackTrace();
            }

            shutDown(false);
        }

        public void shutDown(boolean sendCloseConnectionMessage) {
            Log.traceCall();
            if (!stopped) {
                stopped = true;
                connection.shutDown(sendCloseConnectionMessage);
            }
        }


        public synchronized Socket getSocket() {
            return socket;
        }

        public String getConnectionInfo() {
            return connection.toString();
        }

        public void stop() {
            Log.traceCall();
            this.stopped = true;
        }

        public synchronized ConnectionListener.Reason getShutDownReason() {
            return shutDownReason;
        }

        @Override
        public String toString() {
            return "SharedSpace{" +
                    ", socket=" + socket +
                    ", illegalRequests=" + illegalRequests +
                    ", lastActivityDate=" + lastActivityDate +
                    '}';
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // InputHandler
    ///////////////////////////////////////////////////////////////////////////////////////////

    // Runs in same thread as Connection
    private static class InputHandler implements Runnable {
        private static final Logger log = LoggerFactory.getLogger(InputHandler.class);

        private final SharedSpace sharedSpace;
        private final ObjectInputStream objectInputStream;
        private final String portInfo;
        private final MessageListener messageListener;
        private final boolean useCompression;

        private volatile boolean stopped;

        public InputHandler(SharedSpace sharedSpace, ObjectInputStream objectInputStream, String portInfo, MessageListener messageListener, boolean useCompression) {
            this.useCompression = useCompression;
            Log.traceCall();
            this.sharedSpace = sharedSpace;
            this.objectInputStream = objectInputStream;
            this.portInfo = portInfo;
            this.messageListener = messageListener;
        }

        public void stop() {
            Log.traceCall();
            stopped = true;
        }

        @Override
        public void run() {
            Log.traceCall();
            try {
                Thread.currentThread().setName("InputHandler-" + portInfo);
                while (!stopped && !Thread.currentThread().isInterrupted()) {
                    try {
                        log.trace("InputHandler waiting for incoming messages connection=" + sharedSpace.getConnectionInfo());
                        Object rawInputObject = objectInputStream.readObject();
                        log.trace("New data arrived at inputHandler.Connection=" + sharedSpace.getConnectionInfo());

                        log.info("\n\n<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n" +
                                "New data arrived at inputHandler.\nReceived object={}"
                                + "\n<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n", rawInputObject);

                        int size = ByteArrayUtils.objectToByteArray(rawInputObject).length;
                        if (size > getMaxMsgSize()) {
                            sharedSpace.reportIllegalRequest(IllegalRequest.MaxSizeExceeded);
                            return;
                        }

                        Serializable serializable = null;
                        if (useCompression) {
                            if (rawInputObject instanceof byte[]) {
                                byte[] compressedObjectAsBytes = (byte[]) rawInputObject;
                                size = compressedObjectAsBytes.length;
                                //log.trace("Read object compressed data size: " + size);
                                serializable = Utils.decompress(compressedObjectAsBytes);
                            } else {
                                sharedSpace.reportIllegalRequest(IllegalRequest.InvalidDataType);
                            }
                        } else {
                            if (rawInputObject instanceof Serializable) {
                                serializable = (Serializable) rawInputObject;
                            } else {
                                sharedSpace.reportIllegalRequest(IllegalRequest.InvalidDataType);
                            }
                        }
                        //log.trace("Read object decompressed data size: " + ByteArrayUtils.objectToByteArray(serializable).length);

                        // compressed size might be bigger theoretically so we check again after decompression
                        if (size > getMaxMsgSize()) {
                            sharedSpace.reportIllegalRequest(IllegalRequest.MaxSizeExceeded);
                            return;
                        }
                        if (!(serializable instanceof Message)) {
                            sharedSpace.reportIllegalRequest(IllegalRequest.InvalidDataType);
                            return;
                        }

                        Message message = (Message) serializable;
                        if (message.networkId() != Version.getNetworkId()) {
                            sharedSpace.reportIllegalRequest(IllegalRequest.WrongNetworkId);
                            return;
                        }

                        sharedSpace.updateLastActivityDate();
                        if (message instanceof CloseConnectionMessage) {
                            log.info("CloseConnectionMessage received on connection {}", sharedSpace.connection);
                            stopped = true;
                            sharedSpace.shutDown(false);
                        } else if (!stopped) {
                            messageListener.onMessage(message, null);
                        }
                    } catch (IOException | ClassNotFoundException | NoClassDefFoundError e) {
                        stopped = true;
                        sharedSpace.handleConnectionException(e);
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
                stopped = true;
                sharedSpace.handleConnectionException(new Exception(t));
            }
        }

        @Override
        public String toString() {
            return "InputHandler{" +
                    "sharedSpace=" + sharedSpace +
                    ", port=" + portInfo +
                    ", stopped=" + stopped +
                    '}';
        }
    }
}