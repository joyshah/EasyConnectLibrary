package com.easyconnectlib.easyconnectlib.client.socket.lib;

import android.util.Log;

import com.easyconnectlib.easyconnectlib.client.callbacks.SocketConnectionListener;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ClientSocketConnection {

    private final String TAG = ClientSocketConnection.class.getSimpleName();
    public static final int READ_TIMEOUT = 100;
    public static final int READ_BUFFER_SIZE = 10 * 1024;
    private SocketChannel mSocketChannel;
    private SocketConnectionListener mSocketConnectionListener;
    private boolean isConnectToSocketRunning;
    private boolean isReadThreadRunning = true;

    public ClientSocketConnection() {
        mSocketConnectionListener = null;
    }

    public ClientSocketConnection(SocketConnectionListener socketConnectionListener) {
        mSocketConnectionListener = socketConnectionListener;
    }

    public void registerSocketConnectionListener(SocketConnectionListener socketConnectionListener) {
        mSocketConnectionListener = socketConnectionListener;
    }

    public void unRegisterSocketConnectionListener() {
        mSocketConnectionListener = null;
    }

    public void openSocket(final String serverIp, final int port) {
        isConnectToSocketRunning = false;

        if (!isSocketChannelConnected()) {
            if (!isConnectToSocketRunning) {
                isConnectToSocketRunning = true;
                if (mSocketConnectionListener != null)
                    mSocketConnectionListener.onClientConnecting(serverIp,port);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mSocketChannel = null;
                            mSocketChannel = SocketChannel.open(new InetSocketAddress(serverIp, port));
                            mSocketChannel.configureBlocking(false);

                            if (mSocketChannel != null) {
                                if (mSocketChannel.isConnected() && mSocketChannel.socket() != null && mSocketChannel.socket().isConnected()) {
                                    new Thread(new ReadData()).start();

                                    if (mSocketConnectionListener != null) {
                                        mSocketConnectionListener.onClientConnected(serverIp,port);
                                    }
                                } else {
                                    if (mSocketConnectionListener != null)
                                        mSocketConnectionListener.onClientSocketError(SocketConnectionListener.SOCKET_ERROR.CONNECTION_ERROR);
                                }
                            }
                        } catch (ConnectException ex) {
                            ex.printStackTrace();
                            if (mSocketConnectionListener != null)
                                mSocketConnectionListener.onClientSocketError(SocketConnectionListener.SOCKET_ERROR.CONNECTION_ERROR);
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (mSocketConnectionListener != null)
                                mSocketConnectionListener.onClientSocketError(SocketConnectionListener.SOCKET_ERROR.INTERNAL_ERROR);
                        } finally {
                            isConnectToSocketRunning = false;
                        }
                    }
                }).start();
            }
        } else {
            String ipAddress;
            try {
                ipAddress = mSocketChannel.socket().getInetAddress().toString();
                if (mSocketConnectionListener != null && ipAddress != null) {
                    mSocketConnectionListener.onClientConnected(ipAddress,port);
                } else {
                    if (mSocketConnectionListener != null)
                        mSocketConnectionListener.onClientSocketError(SocketConnectionListener.SOCKET_ERROR.INTERNAL_ERROR);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (mSocketConnectionListener != null)
                    mSocketConnectionListener.onClientSocketError(SocketConnectionListener.SOCKET_ERROR.INTERNAL_ERROR);
            }
        }
    }

    private boolean isSocketChannelConnected() {
        return mSocketChannel != null && mSocketChannel.isConnected();
    }

    public int writeData(final ByteBuffer byteBuffer) throws InterruptedException, ExecutionException {
        final ExecutorService service = Executors.newFixedThreadPool(1);
        final Future<Integer> writtenValue = service.submit(new WriteDataUsingCallable(byteBuffer));
        return writtenValue.get();
    }

    private class WriteDataUsingCallable implements Callable<Integer> {
        private ByteBuffer byteBuffer;
        private int sentValue;

        private WriteDataUsingCallable(ByteBuffer byteBuffer) {
            this.byteBuffer = byteBuffer;
            this.sentValue = 0;
        }

        public Integer call() {
            try {
                if (mSocketChannel != null && mSocketChannel.isConnected()) {
                    while (byteBuffer.hasRemaining()) {
                        this.sentValue = this.sentValue + mSocketChannel.write(byteBuffer);
                    }
                } else {
                    if (mSocketConnectionListener != null)
                        mSocketConnectionListener.onClientSocketError(SocketConnectionListener.SOCKET_ERROR.NOT_CONNECTED);
                }
            } catch (IOException e) {
                e.printStackTrace();
                mSocketChannel = null;
                isReadThreadRunning = false;
                if (mSocketConnectionListener != null)
                    mSocketConnectionListener.onClientSocketError(SocketConnectionListener.SOCKET_ERROR.CONNECTION_ERROR);
            }

            return (this.sentValue);
        }
    }

    public void closeSocket() {
        try {
            isReadThreadRunning = false;
            if (mSocketChannel != null) {
                mSocketChannel.close();
                mSocketChannel = null;
                if (mSocketConnectionListener != null) {
                    mSocketConnectionListener.onClientSocketClose();
                }
            } else {
                if (mSocketConnectionListener != null)
                    mSocketConnectionListener.onClientSocketError(SocketConnectionListener.SOCKET_ERROR.NOT_CONNECTED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ReadData implements Runnable {


        private ReadData() {
            isReadThreadRunning = true;
        }


        @Override
        public void run() {
            ByteBuffer readBuffer = ByteBuffer.allocateDirect(READ_BUFFER_SIZE);
            try {
                while (isReadThreadRunning) {
                    readBuffer.clear();
                    if (mSocketChannel.read(readBuffer) >= 0) {
                        readBuffer.flip();
                        if (mSocketConnectionListener != null && readBuffer.hasRemaining()) {
                            byte[] bytes = new byte[readBuffer.remaining()];
                            readBuffer.get(bytes);
                            String stringData = new String(bytes);
                            mSocketConnectionListener.onDataReceived(readBuffer, stringData);
                            Log.i(TAG, "on Data received :" + stringData);
                        }
                    } else {
                        isReadThreadRunning = false;
                        mSocketChannel = null;
                        if (mSocketConnectionListener != null)
                            mSocketConnectionListener.onClientSocketError(SocketConnectionListener.SOCKET_ERROR.CONNECTION_ERROR);
                    }
                    Thread.sleep(READ_TIMEOUT);
                }
            } catch (Exception e) {
                e.printStackTrace();
                mSocketChannel = null;
                if (mSocketConnectionListener != null)
                    mSocketConnectionListener.onClientSocketError(SocketConnectionListener.SOCKET_ERROR.INTERNAL_ERROR);
            }
        }
    }
}
