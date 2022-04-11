package com.example.myfirstapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    DatagramSocket socket = null;
    DatagramSocket s_listener = null;
    boolean is_halted = true;
    Thread recive_thread = null;
    Thread recive_thread_listener = null;
    int remote_port = 8888;
    InetAddress remote_addr = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            socket = new DatagramSocket();
            s_listener = new DatagramSocket(8888);
        } catch (SocketException ex) {
            TextView txtMsg = findViewById(R.id.txtMsg);
            txtMsg.setText("SocErr: " + ex.toString());
        } catch (SecurityException ex) {
            TextView txtMsg = findViewById(R.id.txtMsg);
            txtMsg.setText("Sec Err: " + ex.toString());
        }
        TextView txtMsg = findViewById(R.id.txtMsg);
        EditText edtIP = findViewById(R.id.edtIp);
        if (socket != null) {
            txtMsg.setText("Da");
            recive_thread = new Thread(() -> {
                while (!recive_thread.isInterrupted()) {
                    int buf_len = 500;
                    byte[] buf = new byte[buf_len];
                    DatagramPacket pak = new DatagramPacket(buf, buf_len);
                    try {
                        socket.receive(pak);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String msg = new String(buf, StandardCharsets.US_ASCII);
                    txtMsg.setText(msg.replace('\t', '\n'));
                    edtIP.setText(pak.getAddress().toString().replace("/", ""));
                    edtIP.setEnabled(false);
                    remote_addr = pak.getAddress();
                    if (!is_halted) {
                        sendMsg("CON", pak.getAddress(), remote_port);
                    }
                }
            });
            recive_thread.start();
        }
        if (s_listener != null) {
            txtMsg.setText("Listening");
            recive_thread_listener = new Thread(() -> {
                int buf_len = 500;
                byte[] buf = new byte[buf_len];
                DatagramPacket pak = new DatagramPacket(buf, buf_len);
                try {
                    s_listener.receive(pak);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String msg = new String(buf, StandardCharsets.US_ASCII);
                txtMsg.setText(msg.replace('\t','\n'));
                edtIP.setText(pak.getAddress().toString().replace("/", ""));
                edtIP.setEnabled(false);
                remote_addr = pak.getAddress();
                sendMsg("CON", pak.getAddress(), remote_port);
            });
            recive_thread_listener.start();
        }
    }

    public void onHalt(View view) {
        EditText edtIp = findViewById(R.id.edtIp);
        Button btnResume = findViewById(R.id.btnResume);
        Button btnHalt = findViewById(R.id.btnHalt);
        if (edtIp.isEnabled()) {
            try {
                remote_addr = InetAddress.getByName(edtIp.getText().toString());
                btnHalt.setText("HALT");
                btnResume.setEnabled(true);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            }
        } else {
            btnHalt.setText("HALT");
            btnResume.setEnabled(true);
        }
        byte[] buf = {1, 2, 3};
        sendMsg("HALT", remote_addr, remote_port);
        is_halted = true;
    }

    public void onResume(View view) {
        sendMsg("CON", remote_addr, remote_port);
        is_halted = false;
    }

    private void sendMsg(String msg, InetAddress addr, int remote_port) {
        byte[] b_msg = msg.getBytes(StandardCharsets.US_ASCII);
        Thread thd = new Thread(() -> {
            DatagramPacket pak = new DatagramPacket(b_msg, b_msg.length, addr, remote_port);
            try {
                socket.send(pak);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thd.start();
    }
}