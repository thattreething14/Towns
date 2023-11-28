package site.albaniacraft.towns.web

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler

class ServerThread(private val port: Int, private val contextHandler: ServletContextHandler) : Thread() {
    override fun run() {
        // Create the Jetty server
        val server = Server(port)

        // Set the context handler for the server
        server.handler = contextHandler

        // Bind to all available network interfaces
        server.setAttribute("org.eclipse.jetty.server.RequestLog.REFERER", "0.0.0.0")

        // Start the server
        try {
            server.start()
            server.join()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            server.destroy()
        }
    }
}
