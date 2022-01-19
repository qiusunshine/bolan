package utils

import android.content.Context
import android.net.wifi.WifiManager
import com.hd.tvpro.util.ToastMgr
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*


object IPUtil {

    fun getIP(context: Context): String {
        try {
            val wifiManager = context.getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
            //判断wifi是否开启
            val wifiInfo = wifiManager.connectionInfo
            var ipAddress = 0
            if (wifiInfo != null) {
                ipAddress = wifiInfo.ipAddress
            }
            return intToIp(ipAddress)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastMgr.shortBottomCenter(context, "出错：$e")
            try {
                return getLocalIPAddress()
            } catch (e1: Exception) {
                e1.printStackTrace()
                ToastMgr.shortBottomCenter(context, "出错：$e")
            }
        }
        return "127.0.0.1"
    }

    private fun getLocalIPAddress(): String {
        try {
            val mEnumeration = NetworkInterface
                .getNetworkInterfaces()
            while (mEnumeration.hasMoreElements()) {
                val intf = mEnumeration.nextElement()
                val enumIPAddr = intf
                    .inetAddresses
                while (enumIPAddr.hasMoreElements()) {
                    val inetAddress = enumIPAddr.nextElement()
                    // 如果不是回环地址
                    if (!inetAddress.isLoopbackAddress) {
                        // 直接返回本地IP地址
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (ex: SocketException) {
            System.err.print("error")
        }
        return "127.0.0.1"
    }

    @Throws(SocketException::class)
    fun getLocalINetAddress(): InetAddress? {
        val mEnumeration = NetworkInterface
            .getNetworkInterfaces()
        while (mEnumeration.hasMoreElements()) {
            val intf = mEnumeration.nextElement()
            val enumIPAddr = intf
                .inetAddresses
            while (enumIPAddr.hasMoreElements()) {
                val inetAddress = enumIPAddr.nextElement()
                // 如果不是回环地址
                if (!inetAddress.isLoopbackAddress) {
                    // 直接返回本地IP地址
                    return inetAddress
                }
            }
        }
        throw SocketException("获取本地IP地址失败！")
    }

    private fun intToIp(i: Int): String {
        return (i and 0xFF).toString() + "." +
                (i shr 8 and 0xFF) + "." +
                (i shr 16 and 0xFF) + "." +
                (i shr 24 and 0xFF)
    }

    fun getIP(): List<String> {
        val list = ArrayList<InetAddress>()
        // 遍历所有的网络接口
        val ifaces: Enumeration<*> = NetworkInterface.getNetworkInterfaces()
        while (ifaces.hasMoreElements()) {
            val iface = ifaces.nextElement() as NetworkInterface
            // 在所有的接口下再遍历IP
            val inetAddrs: Enumeration<*> = iface.inetAddresses
            while (inetAddrs.hasMoreElements()) {
                val inetAddr = inetAddrs.nextElement() as InetAddress
                if (!inetAddr.isLoopbackAddress && inetAddr is Inet4Address) { // 排除loopback类型地址
                    if (!inetAddr.hostAddress.contains(":")) {
                        list.add(inetAddr)
                    }
                }
            }
        }
        return list.map { it.hostAddress }
    }
}