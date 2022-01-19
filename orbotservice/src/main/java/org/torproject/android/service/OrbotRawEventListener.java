package org.torproject.android.service;

import static org.torproject.jni.TorService.STATUS_ON;
import static org.torproject.jni.TorService.STATUS_STARTING;

import android.text.TextUtils;

import net.freehaven.tor.control.RawEventListener;
import net.freehaven.tor.control.TorControlCommands;

import org.torproject.android.service.util.ExternalIPFetcher;
import org.torproject.android.service.util.Prefs;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class OrbotRawEventListener implements RawEventListener {

    private OrbotService mService;
    private long mTotalBandwidthWritten, mTotalBandwidthRead;

    private final Map<String, Node> hmBuiltNodes;


    OrbotRawEventListener(OrbotService orbotService) {
        mService = orbotService;
        mTotalBandwidthRead = 0;
        mTotalBandwidthWritten = 0;
        hmBuiltNodes = new HashMap<>();
    }

    @Override
    public void onEvent(String keyword, String data) {
        String[] payload = data.split(" ");
        if (TorControlCommands.EVENT_BANDWIDTH_USED.equals(keyword)) {
            handleBandwidth(Long.parseLong(payload[0]), Long.parseLong(payload[1]));
        } else if (TorControlCommands.EVENT_NEW_DESC.equals(keyword)) {
            handleNewDescriptors(payload);
        } else if (TorControlCommands.EVENT_STREAM_STATUS.equals(keyword)) {
            handleStreamStatus(payload[1], payload[0], payload[3]);
        } else if (TorControlCommands.EVENT_CIRCUIT_STATUS.equals(keyword)) {
            String status = payload[1];
            String circuitId = payload[0];
            String path;
            if (payload.length < 3 || status.equals(TorControlCommands.CIRC_EVENT_LAUNCHED))
                path = "";
            else path = payload[2];
            handleCircuitStatus(status, circuitId, path);
        } else if (TorControlCommands.EVENT_OR_CONN_STATUS.equals(keyword)) {
            handleConnectionStatus(payload[1], payload[0]);
        } else if (TorControlCommands.EVENT_DEBUG_MSG.equals(keyword) || TorControlCommands.EVENT_INFO_MSG.equals(keyword) ||
                TorControlCommands.EVENT_NOTICE_MSG.equals(keyword) || TorControlCommands.EVENT_WARN_MSG.equals(keyword) ||
                TorControlCommands.EVENT_ERR_MSG.equals(keyword)) {
            handleDebugMessage(keyword, data);
        } else {
            String unrecognized = "Message (" + keyword + "): " + data;
            mService.logNotice(unrecognized);
        }
    }

    private void handleBandwidth(long read, long written) {
        String sb = OrbotService.formatBandwidthCount(mService, read) + " \u2193" + " / " +
                OrbotService.formatBandwidthCount(mService, written) + " \u2191";

        int icon = read == 0 && written == 0 ? R.drawable.ic_stat_tor : R.drawable.ic_stat_tor_xfer;

        mService.showToolbarNotification(sb, OrbotService.NOTIFY_ID, icon);

        mTotalBandwidthWritten += written;
        mTotalBandwidthRead += read;

        mService.sendCallbackBandwidth(written, read, mTotalBandwidthWritten, mTotalBandwidthRead);

    }

    private void handleNewDescriptors(String[] descriptors) {
        for (String descriptor : descriptors)
            mService.debug("descriptors: " + descriptor);
    }

    private void handleStreamStatus(String status, String streamId, String target) {
        String streamStatusMessage = "StreamStatus (" + streamId + "): " + status;
        mService.debug(streamStatusMessage);
    }

    private void handleCircuitStatus(String circuitStatus, String circuitId, String path) {
        /* once the first circuit is complete, then announce that Orbot is on*/
        if (mService.getCurrentStatus() == STATUS_STARTING && TextUtils.equals(circuitStatus, "BUILT"))
            mService.sendCallbackStatus(STATUS_ON);

        if (!Prefs.useDebugLogging()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("Circuit (");
        sb.append((circuitId));
        sb.append(") ");
        sb.append(circuitStatus);
        sb.append(": ");

        StringTokenizer st = new StringTokenizer(path, ",");
        Node node;

        boolean isFirstNode = true;
        int nodeCount = st.countTokens();

        while (st.hasMoreTokens()) {
            String nodePath = st.nextToken();
            String nodeId = null, nodeName = null;

            String[] nodeParts;

            if (nodePath.contains("="))
                nodeParts = nodePath.split("=");
            else
                nodeParts = nodePath.split("~");

            if (nodeParts.length == 1) {
                nodeId = nodeParts[0].substring(1);
                nodeName = nodeId;
            } else if (nodeParts.length == 2) {
                nodeId = nodeParts[0].substring(1);
                nodeName = nodeParts[1];
            }

            if (nodeId == null)
                continue;

            node = hmBuiltNodes.get(nodeId);

            if (node == null) {
                node = new Node();
                node.id = nodeId;
                node.name = nodeName;
            }

            node.status = circuitStatus;

            sb.append(node.name);

            if (!TextUtils.isEmpty(node.ipAddress))
                sb.append("(").append(node.ipAddress).append(")");

            if (st.hasMoreTokens())
                sb.append(" > ");

            if (circuitStatus.equals("EXTENDED")) {

                if (isFirstNode) {
                    hmBuiltNodes.put(node.id, node);

                    if (node.ipAddress == null && (!node.isFetchingInfo) && Prefs.useDebugLogging()) {
                        node.isFetchingInfo = true;
                        mService.exec(new ExternalIPFetcher(mService, node, OrbotService.mPortHTTP));
                    }

                    isFirstNode = false;
                }
            } else if (circuitStatus.equals("BUILT")) {
                //   mService.logNotice(sb.toString());

                if (Prefs.useDebugLogging() && nodeCount > 3)
                    mService.debug(sb.toString());
            } else if (circuitStatus.equals("CLOSED")) {
                //  mService.logNotice(sb.toString());
                hmBuiltNodes.remove(node.id);
            }

        }
    }

    private void handleConnectionStatus(String status, String unparsedNodeName) {
        String message = "orConnStatus (" + parseNodeName(unparsedNodeName) + "): " + status;
        mService.debug(message);
    }

    private void handleDebugMessage(String severity, String message) {
        if (severity.equalsIgnoreCase("debug"))
            mService.debug(severity + ": " + message);
        else
            mService.logNotice(severity + ": " + message);
    }

    public Map<String, Node> getNodes() {
        return hmBuiltNodes;
    }

    public static class Node {
        public String status;
        public String id;
        public String name;
        public String ipAddress;
        public String country;
        public String organization;

        public boolean isFetchingInfo = false;
    }


    private static String parseNodeName(String node) {
        if (node.indexOf('=') != -1) {
            return node.substring(node.indexOf("=") + 1);
        } else if (node.indexOf('~') != -1) {
            return node.substring(node.indexOf("~") + 1);
        }
        return node;
    }

}
