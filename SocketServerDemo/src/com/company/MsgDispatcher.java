package com.company;

import com.company.bean.MsgBean;
import com.company.utils.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MsgDispatcher {

    public static final ExecutorService readThreadPool = Executors.newCachedThreadPool();

    public static final ExecutorService writeThreadPool = Executors.newCachedThreadPool();

    private static final Map<String, OutputStream> outerStream = new TreeMap<>();

    private Socket mSocket;

    private InputStream mInputStream;

    private OutputStream mOutputStream;

    private Future mReadFuture;

    private Future mWriteFuture;

    private ByteBuffer mRemainingBuf;

    public MsgDispatcher(Socket socket) {
        this.mSocket = socket;
        try {
            this.mInputStream = socket.getInputStream();
            this.mOutputStream = socket.getOutputStream();
            outerStream.put(mSocket.getInetAddress().getHostAddress(), mOutputStream);
            //start io thread
            mReadFuture = readThreadPool.submit(new Reader());
            mWriteFuture = writeThreadPool.submit(new Writer());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class Writer implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    MsgBean msgBean = MessageQueue.getIns().take();

                    if (msgBean != null) {
                        Log.bytes("write from:" + msgBean.getFromWho() + " to all data:", msgBean.getBytes());
                        Log.i("write from:" + msgBean.getFromWho() + " to all data:"
                                + new String(msgBean.getBytes(), Charset.forName("utf-8")));

                        Iterator<String> it = outerStream.keySet().iterator();
                        while (it.hasNext()) {
                            String key = it.next();
                            OutputStream os = outerStream.get(key);
                            os.write(msgBean.getBytes());
                            os.flush();
                        }
                    }
                }
            } catch (Exception e) {
            } finally {
                outerStream.remove(mSocket.getInetAddress().getHostAddress());
                try {
                    if (mOutputStream != null) {
                        mOutputStream.close();
                    }
                    mSocket.close();
                    Log.e("client is disconnect");
                } catch (IOException e) {
                    Log.e("client is disconnect with exception");
                }
                mWriteFuture.cancel(true);
            }
        }
    }

    private class Reader implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    ByteBuffer totalBuf = null;
                    try {
                        ByteBuffer headBuf = ByteBuffer.allocate(4);
                        if (mRemainingBuf != null) {
                            mRemainingBuf.flip();
                            int length = Math.min(mRemainingBuf.remaining(), 4);
                            headBuf.put(mRemainingBuf.array(), 0, length);
                            if (length < 4) {
                                //there are no data left
                                mRemainingBuf = null;
                                for (int i = 0; i < 4 - length; i++) {
                                    headBuf.put((byte) mInputStream.read());
                                }
                            } else {
                                mRemainingBuf.position(4);
                            }
                        } else {
                            for (int i = 0; i < headBuf.capacity(); i++) {
                                headBuf.put((byte) mInputStream.read());
                            }
                        }
                        headBuf.flip();
                        int bodyLength = headBuf.getInt();
                        byte[] bodyArray = new byte[0];
                        if (bodyLength > 0) {
                            if (bodyLength > 10 * 1024 * 1024) {//大于最大的读取容量,说明数据有问题
                                throw new IllegalArgumentException("we can't read data bigger than " + 10 + "Mb");
                            }
                            ByteBuffer byteBuffer = ByteBuffer.allocate(bodyLength);
                            if (mRemainingBuf != null) {
                                int bodyStartPosition = mRemainingBuf.position();
                                int length = Math.min(mRemainingBuf.remaining(), bodyLength);
                                byteBuffer.put(mRemainingBuf.array(), bodyStartPosition, length);
                                mRemainingBuf.position(bodyStartPosition + length);
                                if (length == bodyLength) {
                                    if (mRemainingBuf.remaining() > 0) {//there are data left
                                        mRemainingBuf = ByteBuffer.allocate(mRemainingBuf.remaining());
                                        mRemainingBuf
                                                .put(mRemainingBuf.array(), mRemainingBuf.position(), mRemainingBuf.remaining());
                                    } else {//there are no data left
                                        mRemainingBuf = null;
                                    }
                                    //cause this time data from remaining buffer not from channel.
                                    bodyArray = byteBuffer.array();
                                    return;
                                } else {//there are no data left in buffer and some data pieces in channel
                                    mRemainingBuf = null;
                                }
                            }
                            readBodyFromChannel(byteBuffer);
                            bodyArray = byteBuffer.array();
                        } else if (bodyLength == 0) {
                            bodyArray = new byte[0];
                        } else if (bodyLength < 0) {
                            throw new IllegalArgumentException(
                                    "this socket input stream has some problem,wrong body length " + bodyLength
                                            + ",we'll disconnect");
                        }
                        totalBuf = ByteBuffer.allocate(4 + bodyArray.length);
                        headBuf.flip();
                        totalBuf.put(headBuf);
                        totalBuf.put(bodyArray);
                        totalBuf.flip();
                    } catch (Exception e) {
                        throw e;
                    }

                    if(totalBuf != null){
                        Log.bytes("read from:"+mSocket.getInetAddress().getHostAddress()+" data:", totalBuf.array());
                        Log.i("read from:"+mSocket.getInetAddress().getHostAddress()+" data:"
                                + new String(totalBuf.array(), Charset.forName("utf-8")));
                        MsgBean msgBean = new MsgBean(mSocket.getInetAddress().getHostAddress(), null, totalBuf.array());
                        MessageQueue.getIns().offer(msgBean);
                    }
                }
            } catch (Exception e) {
            } finally {
                try {
                    if (mInputStream != null) {
                        mInputStream.close();
                    }

                } catch (IOException e) {
                }
                mReadFuture.cancel(true);
            }
        }
    }


    private void readBodyFromChannel(ByteBuffer byteBuffer) throws IOException {
        while (byteBuffer.hasRemaining()) {
            try {
                byte[] bufArray = new byte[100];
                int len = mInputStream.read(bufArray);
                if (len < 0) {
                    break;
                }
                int remaining = byteBuffer.remaining();
                if (len > remaining) {
                    byteBuffer.put(bufArray, 0, remaining);
                    mRemainingBuf = ByteBuffer.allocate(len - remaining);
                    mRemainingBuf.put(bufArray, remaining, len - remaining);
                } else {
                    byteBuffer.put(bufArray, 0, len);
                }
            } catch (Exception e) {
                throw e;
            }
        }
    }

}
