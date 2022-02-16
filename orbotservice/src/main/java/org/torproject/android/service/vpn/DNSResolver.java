package org.torproject.android.service.vpn;

import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.io.IOException;

public class DNSResolver {

    private final SimpleResolver mResolver;

    public DNSResolver(String localDns, int localPort) throws IOException {
        mResolver = new SimpleResolver(localDns);
        mResolver.setPort(localPort);
    }

    public byte[] processDNS(byte[] payload) throws IOException {
        Message msgRequest = new Message(payload);

        if (msgRequest.getQuestion() != null) {
            Message queryMessage = Message.newQuery(msgRequest.getQuestion());
            Message answer = mResolver.send(queryMessage);
            return answer.toWire();
        }
        return null;
    }

}
