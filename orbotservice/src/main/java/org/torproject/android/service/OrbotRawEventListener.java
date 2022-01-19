package org.torproject.android.service;

import android.util.Log;

import net.freehaven.tor.control.RawEventListener;
import net.freehaven.tor.control.TorControlCommands;

class OrbotRawEventListener implements RawEventListener {

    private OrbotService mService;
    private long mTotalBandwidthWritten, mTotalBandwidthRead;

    OrbotRawEventListener(OrbotService orbotService) {
        mService = orbotService;
        mTotalBandwidthRead = 0;
        mTotalBandwidthWritten = 0;
    }

    @Override
    public void onEvent(String keyword, String data) {
        String[] payload = data.split(" ");
        if (TorControlCommands.EVENT_BANDWIDTH_USED.equals(keyword)) {
            handleBandwidth(Long.parseLong(payload[0]), Long.parseLong(payload[1]));
        } else if (TorControlCommands.EVENT_NEW_DESC.equals(keyword)) {
            handleNewDescriptors(payload);
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


}
