package com.yaindustries.fileshare.interfaces;

import java.net.InetAddress;

public interface NsdTaskInvoker {

    void onNsdTaskSuccessful(InetAddress address, int port);

    void onNsdTaskFailed();
}
