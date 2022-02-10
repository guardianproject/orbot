package org.torproject.android.service.vpn;

public class Packet {
    public static class IP {
        public byte     version;
        public byte     ihl;
        public byte     tos;
        public int      tot_len;    // uint16_t
        public short    id;
        public short    frag_off;
        public short    ttl;        // uint8_t
        public short    protocol;  // uint8_t
        public short    check;
        public int      saddr;
        public int      daddr;

        public IP() {
        }

        public IP(byte[] data, int offset, int length) {
            if (length < 20)
                return;

            version = (byte)((data[offset] & 0xF0) >> 4);
            ihl = (byte)((data[offset] & 0xF) << 2);
            tos = (byte)(data[offset + 1] & 0xFF);
            tot_len = (int)((data[offset + 2] & 0xFF) << 8 | (data[offset + 3] & 0xFF));
            id = (short)((data[offset + 4] & 0xFF) << 8 | (data[offset + 5] & 0xFf));
            frag_off = (short)((data[offset + 6] & 0xFF) << 8 | (data[offset + 7] & 0xFF));
            ttl = (short)(data[offset + 8] & 0xFF);
            protocol = (short)(data[offset + 9] & 0xFF);
            check = (short)((data[offset + 10] & 0xFF) << 8 | (data[offset + 11] & 0xFF));
            saddr = (int)((data[offset + 12] & 0xFF) << 24 | (data[offset + 13] & 0xFF) << 16 | (data[offset + 14] & 0xFF) << 8 | (data[offset + 15] & 0xFF));
            daddr = (int)((data[offset + 16] & 0xFF) << 24 | (data[offset + 17] & 0xFF) << 16 | (data[offset + 18] & 0xFF) << 8 | (data[offset + 19] & 0xFF));
        }

        public void encode(byte[] data, int offset, int length) {
            if (length < 20)
                return;

            data[offset] = (byte)((version & 0xF) << 4 | ((ihl >> 2) & 0xF));
            data[offset + 1] = (byte)(tos & 0xFF);
            data[offset + 2] = (byte)((tot_len & 0xFF00) >> 8);
            data[offset + 3] = (byte)(tot_len & 0xFF);
            data[offset + 4] = (byte)((id & 0xFF00) >> 8);
            data[offset + 5] = (byte)(id & 0xFF);
            data[offset + 6] = (byte)((frag_off & 0xFF00) >> 8);
            data[offset + 7] = (byte)(frag_off & 0xFF);
            data[offset + 8] = (byte)(ttl & 0xFF);
            data[offset + 9] = (byte)(protocol & 0xFF);
            data[offset + 10] = (byte)((check & 0xFF00) >> 8);
            data[offset + 11] = (byte)(check & 0xFF);
            data[offset + 12] = (byte)((saddr & 0xFF000000) >> 24);
            data[offset + 13] = (byte)((saddr & 0xFF0000) >> 16);
            data[offset + 14] = (byte)((saddr & 0xFF00) >> 8);
            data[offset + 15] = (byte)(saddr & 0xFF);
            data[offset + 16] = (byte)((daddr & 0xFF000000) >> 24);
            data[offset + 17] = (byte)((daddr & 0xFF0000) >> 16);
            data[offset + 18] = (byte)((daddr & 0xFF00) >> 8);
            data[offset + 19] = (byte)(daddr & 0xFF);
        }
    }

    public static class TCP {
        public int      source;     // uint16_t
        public int      dest;       // uint16_t
        public long     seq;        // uint32_t
        public long     ack_seq;    // uint32_t
        public byte     doff;
        public byte     res1;
        public byte     cwr;
        public byte     ecn;
        public byte     urg;
        public byte     ack;
        public byte     psh;
        public byte     rst;
        public byte     syn;
        public byte     fin;
        public int      window;     // uint16_t
        public short    check;
        public short    urg_ptr;

        public TCP() {
        }

        public TCP(byte[] data, int offset, int length) {
            if (length < 20)
                return;

            source = (int)((data[offset] & 0xFF) << 8 | (data[offset + 1] & 0xFF));
            dest = (int)((data[offset + 2] & 0xFF) << 8 | (data[offset + 3] & 0xFF));
            seq = (int)((data[offset + 4] & 0xFF) << 24 | (data[offset + 5] & 0xFF) << 16 | (data[offset + 6] & 0xFF) << 8 | (data[offset + 7] & 0xFF));
            ack_seq = (int)((data[offset + 8] & 0xFF) << 24 | (data[offset + 9] & 0xFF) << 16 | (data[offset + 10] & 0xFF) << 8 | (data[offset + 11] & 0xFF));
            doff = (byte)(((data[offset + 12] & 0xF0) >> 4) << 2);
            res1 = (byte)(data[offset + 12] & 0xF);
            cwr = (byte)((data[offset + 13] & 0x80) >> 7);
            ecn = (byte)((data[offset + 13] & 0x40) >> 6);
            urg = (byte)((data[offset + 13] & 0x20) >> 5);
            ack = (byte)((data[offset + 13] & 0x10) >> 4);
            psh = (byte)((data[offset + 13] & 0x8) >> 3);
            rst = (byte)((data[offset + 13] & 0x4) >> 2);
            syn = (byte)((data[offset + 13] & 0x2) >> 1);
            fin = (byte)(data[offset + 13] & 0x1);
            window = (int)((data[offset + 14] & 0xFF) << 8 | data[offset + 15] & 0xFF);
            check = (short)((data[offset + 16] & 0xFF) << 8 | data[offset + 17] & 0xFF);
            urg_ptr = (short)((data[offset + 18] & 0xFF) << 8 | data[offset + 19] & 0xFF);
        }

        public void encode(byte[] data, int offset, int length) {
            if (length < 20)
                return;

            data[offset] = (byte)((source & 0xFF00) >> 8);
            data[offset + 1] = (byte)(source & 0xFF);
            data[offset + 2] = (byte)((dest & 0xFF00) >> 8);
            data[offset + 3] = (byte)(dest & 0xFF);
            data[offset + 4] = (byte)((seq & 0xFF000000) >> 24);
            data[offset + 5] = (byte)((seq & 0xFF0000) >> 16);
            data[offset + 6] = (byte)((seq & 0xFF00) >> 8);
            data[offset + 7] = (byte)(seq & 0xFF);
            data[offset + 8] = (byte)((ack_seq & 0xFF000000) >> 24);
            data[offset + 9] = (byte)((ack_seq & 0xFF0000) >> 16);
            data[offset + 10] = (byte)((ack_seq & 0xFF00) >> 8);
            data[offset + 11] = (byte)(ack_seq & 0xFF);
            data[offset + 12] = (byte)((((doff >> 2) & 0xF) << 4) | (res1 & 0xF));
            data[offset + 13] = (byte)(cwr << 7 | ecn << 6 | urg << 5 | ack << 4 | psh << 3 | rst << 2 | syn << 1 | fin);
            data[offset + 14] = (byte)((window & 0xFF00) >> 8);
            data[offset + 15] = (byte)(window & 0xFF);
            data[offset + 16] = (byte)((check & 0xFF00) >> 8);
            data[offset + 17] = (byte)(check & 0xFF);
            data[offset + 18] = (byte)((urg_ptr & 0xFF00) >> 8);
            data[offset + 19] = (byte)(urg_ptr & 0xFF);
        }
    }

    private static long partCksum(long initcksum, byte[] data, int offset, int length) {
        long cksum;
        int idx;
        int odd;

        cksum = initcksum;

        odd = length & 1;
        length -= odd;

        for (idx = 0; idx < length; idx += 2) {
            cksum += (data[offset + idx] & 0xFF) << 8 | (data[offset + idx +1] & 0xFF);
        }

        if (odd != 0) {
            cksum += (data[offset + idx] & 0xFF) << 8;
        }

        while ((cksum >> 16) != 0) {
            cksum = (cksum &0xFFFF) + (cksum >> 16);
        }

        return cksum;
    }

    // offset is ip header offset
    public static void recalcIPCheckSum(byte[] data, int offset, int length) {
        long answer;

        if (length < 20)
            return;

        data[offset + 10] = 0;
        data[offset + 11] = 0;
        answer = partCksum(0, data, offset, length);
        answer = ~answer & 0xFFFF;

        data[offset + 10] = (byte)((answer & 0xFF00) >> 8);
        data[offset + 11] = (byte)((answer & 0xFF));
    }

    // offset is ip header offset
    public static void recalcTCPCheckSum(byte[] data, int offset, int length) {
        long calccksum;

        int ipTotLen = (int)((data[offset + 2] & 0xFF) << 8 | (data[offset + 3] & 0xFF));
        int tcpOffset = offset + ((data[offset] & 0xF) << 2);
        int tcpLen = ipTotLen - ((data[offset] & 0xF) << 2);

        if (length < ipTotLen)
            return;

        byte[] phdr = new byte[4];
        phdr[0] = 0;
        phdr[1] = 6;
        phdr[2] = (byte)((tcpLen >> 8) & 0xFF);
        phdr[3] = (byte)((tcpLen & 0xFF));

        data[tcpOffset + 16] = 0;
        data[tcpOffset + 17] = 0;

        calccksum = partCksum(0L, data, offset + 12,4);
        calccksum = partCksum(calccksum, data, offset + 16,4);
        calccksum = partCksum(calccksum, phdr, 0, 4);
        calccksum = partCksum(calccksum, data, tcpOffset, tcpLen);
        calccksum = ~calccksum & 0xFFFF;

        data[tcpOffset + 16] = (byte)((calccksum & 0xFF00) >> 8);
        data[tcpOffset + 17] = (byte)((calccksum & 0xFF));
    }
}