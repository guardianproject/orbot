package org.torproject.android.service;

import android.text.TextUtils;

import net.freehaven.tor.control.EventHandler;

import org.torproject.android.service.util.ExternalIPFetcher;
import org.torproject.android.service.util.Prefs;

import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by n8fr8 on 9/25/16.
 */
public class TorEventHandler implements EventHandler, TorServiceConstants {

    private final static int BW_THRESDHOLD = 10000;
    private final OrbotService mService;
    private long lastRead = -1;
    private long lastWritten = -1;
    private long mTotalTrafficWritten = 0;
    private long mTotalTrafficRead = 0;
    private final HashMap<String, Node> hmBuiltNodes = new HashMap<>();

    public TorEventHandler(OrbotService service) {
        mService = service;
    }

    public HashMap<String, Node> getNodes() {
        return hmBuiltNodes;
    }

    @Override
    public void message(String severity, String msg) {
        if (severity.equalsIgnoreCase("debug"))
            mService.debug(severity + ": " + msg);
        else
            mService.logNotice(severity + ": " + msg);
    }

    @Override
    public void newDescriptors(List<String> orList) {

        for (String desc : orList)
            mService.debug("descriptors: " + desc);

    }

    @Override
    public void orConnStatus(String status, String orName) {

        String sb = "orConnStatus (" +
                parseNodeName(orName) +
                "): " +
                status;
        mService.debug(sb);
    }

    @Override
    public void streamStatus(String status, String streamID, String target) {

        String sb = "StreamStatus (" +
                (streamID) +
                "): " +
                status;
        mService.debug(sb);
    }

    @Override
    public void unrecognized(String type, String msg) {

        String sb = "Message (" +
                type +
                "): " +
                msg;
        mService.logNotice(sb);
    }

    @Override
    public void bandwidthUsed(long read, long written) {

        if (lastWritten > BW_THRESDHOLD || lastRead > BW_THRESDHOLD) {

            int iconId = R.drawable.ic_stat_tor;

            if (read > 0 || written > 0)
                iconId = R.drawable.ic_stat_tor_xfer;

            String sb = OrbotService.formatBandwidthCount(mService, read) + " \u2193" + " / " +
                    OrbotService.formatBandwidthCount(mService, written) + " \u2191";
            mService.showToolbarNotification(sb, OrbotService.NOTIFY_ID, iconId);

            mTotalTrafficWritten += written;
            mTotalTrafficRead += read;

            mService.sendCallbackBandwidth(written, read, mTotalTrafficWritten, mTotalTrafficRead);

            lastWritten = 0;
            lastRead = 0;
        }

        lastWritten += written;
        lastRead += read;

    }

    public void circuitStatus(String status, String circID, String path) {

        /* once the first circuit is complete, then announce that Orbot is on*/
        if (mService.getCurrentStatus() == STATUS_STARTING && TextUtils.equals(status, "BUILT"))
            mService.sendCallbackStatus(STATUS_ON);

        if (Prefs.useDebugLogging()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Circuit (");
            sb.append((circID));
            sb.append(") ");
            sb.append(status);
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

                node.status = status;

                sb.append(node.name);

                if (!TextUtils.isEmpty(node.ipAddress))
                    sb.append("(").append(node.ipAddress).append(")");

                if (st.hasMoreTokens())
                    sb.append(" > ");

                if (status.equals("EXTENDED")) {

                    if (isFirstNode) {
                        hmBuiltNodes.put(node.id, node);

                        if (node.ipAddress == null && (!node.isFetchingInfo) && Prefs.useDebugLogging()) {
                            node.isFetchingInfo = true;
                            mService.exec(new ExternalIPFetcher(mService, node, OrbotService.mPortHTTP));
                        }

                        isFirstNode = false;
                    }
                } else if (status.equals("BUILT")) {
                    //   mService.logNotice(sb.toString());

                    if (Prefs.useDebugLogging() && nodeCount > 3)
                        mService.debug(sb.toString());
                } else if (status.equals("CLOSED")) {
                    //  mService.logNotice(sb.toString());
                    hmBuiltNodes.remove(node.id);
                }

            }


        }


    }

    private String parseNodeName(String node) {
        if (node.indexOf('=') != -1) {
            return (node.substring(node.indexOf("=") + 1));
        } else if (node.indexOf('~') != -1) {
            return (node.substring(node.indexOf("~") + 1));
        } else
            return node;
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
}
