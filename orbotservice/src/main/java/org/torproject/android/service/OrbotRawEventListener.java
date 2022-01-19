package org.torproject.android.service;

import android.util.Log;

import net.freehaven.tor.control.RawEventListener;
import net.freehaven.tor.control.TorControlCommands;

class OrbotRawEventListener implements RawEventListener {

    private OrbotService mService;
    private long mLastRead, mLastWritten;
    private long mTotalBandwidthWritten, mTotalBandwidthRead;

    private static final long BANDWIDTH_THRESHOLD = 00;

    OrbotRawEventListener(OrbotService orbotService) {
        mService = orbotService;
        mTotalBandwidthRead = 0;
        mTotalBandwidthWritten = 0;
    }

    @Override
    public void onEvent(String keyword, String data) {
        String[] payload = data.split(" ");
        if (keyword.equals(TorControlCommands.EVENT_BANDWIDTH_USED)) {
            Log.d("bim", "OEL keyword " + keyword);
            Log.d("bim", "OEL data " + data);
            handleBandwidth(Long.parseLong(payload[0]), Long.parseLong(payload[1]));
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

}
