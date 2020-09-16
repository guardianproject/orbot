package org.torproject.android.service.util;

import android.util.Log;

import com.offbynull.portmapper.PortMapperFactory;
import com.offbynull.portmapper.gateway.Bus;
import com.offbynull.portmapper.gateway.Gateway;
import com.offbynull.portmapper.gateways.network.NetworkGateway;
import com.offbynull.portmapper.gateways.network.internalmessages.KillNetworkRequest;
import com.offbynull.portmapper.gateways.process.ProcessGateway;
import com.offbynull.portmapper.gateways.process.internalmessages.KillProcessRequest;
import com.offbynull.portmapper.mapper.MappedPort;
import com.offbynull.portmapper.mapper.PortMapper;
import com.offbynull.portmapper.mapper.PortType;

import java.util.List;

public class PortForwarder {

    private boolean shutdown = false;
    private Thread mThread = null;

    public void shutdown() {
        shutdown = true;
    }

    public void forward(final int internalPort, final int externalPort, final long lifetime) throws InterruptedException {

        mThread = new Thread() {
            public void run() {
                try {
                    forwardSync(internalPort, externalPort, lifetime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        mThread.start();
    }


    public void forwardSync(int internalPort, int externalPort, long lifetime) throws InterruptedException {
        // Start gateways
        Gateway network = NetworkGateway.create();
        Gateway process = ProcessGateway.create();
        Bus networkBus = network.getBus();
        Bus processBus = process.getBus();

// Discover port forwarding devices and take the first one found
        List<PortMapper> mappers = PortMapperFactory.discover(networkBus, processBus);
        PortMapper mapper = mappers.get(0);

// Map internal port 12345 to some external port (55555 preferred)
//
// IMPORTANT NOTE: Many devices prevent you from mapping ports that are <= 1024
// (both internal and external ports). Be mindful of this when choosing which
// ports you want to map.
        MappedPort mappedPort = mapper.mapPort(PortType.TCP, internalPort, externalPort, lifetime);
        Log.d(getClass().getName(), "Port mapping added: " + mappedPort);

// Refresh mapping half-way through the lifetime of the mapping (for example,
// if the mapping is available for 40 seconds, refresh it every 20 seconds)
        while (!shutdown) {
            mappedPort = mapper.refreshPort(mappedPort, mappedPort.getLifetime() / 2L);
            Log.d(getClass().getName(), "Port mapping refreshed: " + mappedPort);
            Thread.sleep(mappedPort.getLifetime() * 1000L);
        }

// Unmap port 12345
        mapper.unmapPort(mappedPort);

// Stop gateways
        networkBus.send(new KillNetworkRequest());
        processBus.send(new KillProcessRequest()); // can kill this after discovery
    }
}
