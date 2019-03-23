package core.utils

import groovy.util.logging.Slf4j
import org.apache.commons.mail.EmailAttachment
import org.apache.commons.mail.EmailException
import org.apache.commons.mail.MultiPartEmail

@Slf4j
class Mailer {
    private MultiPartEmail email = new MultiPartEmail()

    Mailer(){
        port().from()
    }

    Mailer smtp(String address){
        email.setHostName(address)
        this
    }

    Mailer port(int portNumber = 25){
        email.setSmtp(portNumber)
        this
    }

    Mailer credentials(String userName, String password){
        email.setAuthentication(userName, password)
        email.setSSLOnConnect(true)
        this
    }

    Mailer from(String emailAddress = "no-repply@automated.mail", String name= ""){
        email.setFrom(emailAddress, name)
        this
    }

    Mailer to(List<String> recipientEmails){
        recipientEmails.each {email.addTo(it)}
        this
    }

    Mailer subject(String title){
        email.setSubject(title)
        this
    }

    Mailer body(String text){
        email.setMsg(text)
        this
    }

    Mailer attach(List<String> filePaths) throws EmailException{
        filePaths.each {path->
            EmailAttachment attachment = new EmailAttachment()
            attachment.setPath(path)
            attachment.setDisposition(EmailAttachment.ATTACHMENT)
            email.attach(attachment)
        }
        this
    }

    boolean send() throws EmailException{
        email.send()
    }

}
