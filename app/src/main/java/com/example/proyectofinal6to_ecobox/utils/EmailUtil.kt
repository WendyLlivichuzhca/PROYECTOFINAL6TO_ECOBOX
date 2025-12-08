package com.example.proyectofinal6to_ecobox.utils

import android.util.Log
import java.util.Properties
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailUtil {

    // --- CONFIGURACIÓN (Reemplaza con tus datos reales) ---
    private const val EMAIL_EMISOR = "mayancelanicole16@gmail.com"
    private const val PASSWORD_EMISOR = "ccty etnv lqrs swkm"

    fun enviarCorreoRecuperacion(destinatario: String, token: String): Boolean {
        return try {
            val props = Properties()
            props["mail.smtp.host"] = "smtp.gmail.com"
            props["mail.smtp.socketFactory.port"] = "465"
            props["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.port"] = "465"

            val session = Session.getDefaultInstance(props, object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(EMAIL_EMISOR, PASSWORD_EMISOR)
                }
            })

            val message = MimeMessage(session)
            message.setFrom(InternetAddress(EMAIL_EMISOR, "Soporte EcoBox"))
            message.addRecipient(Message.RecipientType.TO, InternetAddress(destinatario))
            message.subject = "Restablecer Contraseña - EcoBox"

            // Enlace Mágico (Deep Link)
            val link = "http://www.ecobox-app.com/recuperar?token=$token&email=$destinatario"
            // --- DISEÑO DEL CORREO (HTML) ---
            // Esto es lo que hace que se vea como una "foto" o tarjeta en Gmail
            val contenidoHtml = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #A5D6A7; border-radius: 10px; background-color: #F9FBE7;">
                    
                    <div style="text-align: center; margin-bottom: 20px;">
                        <div style="display: inline-block; background-color: #2E7D32; padding: 15px; border-radius: 50%;">
                           <span style="font-size: 30px;">🌿</span> <!-- Aquí iría tu logo si tuvieras URL -->
                        </div>
                        <h2 style="color: #2E7D32; margin-top: 10px;">EcoBox</h2>
                    </div>

                    <div style="background-color: #ffffff; padding: 20px; border-radius: 8px; text-align: center;">
                        <h3 style="color: #333;">Recuperación de Contraseña</h3>
                        <p style="color: #666; font-size: 16px;">Hola,</p>
                        <p style="color: #666; font-size: 16px;">Hemos recibido una solicitud para restablecer tu contraseña.</p>
                        
                        <br>
                        
                        <!-- ESTE ES EL BOTÓN QUE PARECE FOTO -->
                        <a href="$link" style="background-color: #2E7D32; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px; display: inline-block;">
                           RESTABLECER CONTRASEÑA
                        </a>
                        
                        <br><br>
                        <p style="color: #999; font-size: 12px;">O copia y pega este enlace si el botón no funciona:<br>$link</p>
                    </div>

                    <div style="text-align: center; margin-top: 20px; color: #888; font-size: 12px;">
                        <p>Si no solicitaste este cambio, puedes ignorar este correo.</p>
                        <p>&copy; 2025 EcoBox</p>
                    </div>
                </div>
            """.trimIndent()

            message.setContent(contenidoHtml, "text/html; charset=utf-8")

            Transport.send(message)
            Log.d("EmailUtil", "Correo HTML enviado a $destinatario")
            true

        } catch (e: Exception) {
            Log.e("EmailUtil", "Error enviando correo", e)
            false
        }
    }
}