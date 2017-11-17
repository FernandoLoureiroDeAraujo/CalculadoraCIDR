package br.com.loureiro.misc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by fernando on 06/10/17.
 */
public class CalculateIP {

    private static final String IP_ADDRESS = "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})";
    private static final String SLASH_FORMAT = IP_ADDRESS + "/(\\d{1,3})";
    private static final Pattern addressPattern = Pattern.compile(IP_ADDRESS);
    private static final Pattern cidrPattern = Pattern.compile(SLASH_FORMAT);
    private static final int NBITS = 32;

    private int netmask = 0;
    private int address = 0;
    private int network = 0;
    private int broadcast = 0;

    private boolean inclusiveHostCount = false;

    public CalculateIP(String cidr) {
        calculate(cidr);
    }

    public boolean isInclusiveHostCount() {
        return inclusiveHostCount;
    }

    public void setInclusiveHostCount(boolean inclusiveHostCount) {
        this.inclusiveHostCount = inclusiveHostCount;
    }

    public final class Calculate {
        /* Máscara para converter unsigned int para long (ou seja, mantém 32 bits)*/
        private static final long UNSIGNED_INT_MASK = 0x0FFFFFFFFL;

        private Calculate() {}

        private int netmask()       { return netmask; }
        private int network()       { return network; }
        private int address()       { return address; }
        private int broadcast()     { return broadcast; }

        private long networkLong()  { return network &  UNSIGNED_INT_MASK; }
        private long broadcastLong(){ return broadcast &  UNSIGNED_INT_MASK; }

        private int low() {
            return (isInclusiveHostCount() ? network() : broadcastLong() - networkLong() > 1 ? network() + 1 : 0);
        }

        private int high() {
            return (isInclusiveHostCount() ? broadcast() : broadcastLong() - networkLong() > 1 ? broadcast() -1  : 0);
        }

        public String getBroadcastAddress() {
            return format(toArray(broadcast()));
        }

        public String getNetworkAddress() {
            return format(toArray(network()));
        }

        public String getNetmask() {
            return format(toArray(netmask()));
        }

        public String getAddress() {
            return format(toArray(address()));
        }

        public String getLowAddress() {
            return format(toArray(low()));
        }

        public String getHighAddress() {
            return format(toArray(high()));
        }

        public int getAddressCount() {
            long countLong = getAddressCountLong();
            if (countLong > Integer.MAX_VALUE) {
                throw new RuntimeException("Quantidade de endereços incorreta: " + countLong);
            }

            return (int)countLong;
        }

        public long getAddressCountLong() {
            long b = broadcastLong();
            long n = networkLong();
            long count = b - n + (isInclusiveHostCount() ? 1 : -1);
            return count < 0 ? 0 : count;
        }

        public int asInteger(String address) {
            return toInteger(address);
        }

        public String getCidrSignature() {
            return toCidrNotation(
                    format(toArray(address())),
                    format(toArray(netmask()))
            );
        }

        public String[] getAllAddresses() {
            int ct = getAddressCount();
            String[] addresses = new String[ct];
            if (ct == 0) {
                return addresses;
            }
            for (int add = low(), j=0; add <= high(); ++add, ++j) {
                addresses[j] = format(toArray(add));
            }
            return addresses;
        }

        @Override
        public String toString() {
            final StringBuilder str = new StringBuilder();
            str.append("CIDR Signature:\t[").append(getCidrSignature()).append("]");
            str.append(" Netmask: [").append(getNetmask()).append("]\n");
            str.append("Network:\t[").append(getNetworkAddress()).append("]\n");
            str.append("Broadcast:\t[").append(getBroadcastAddress()).append("]\n");
            str.append("First Address:\t[").append(getLowAddress()).append("]\n");
            str.append("Last Address:\t[").append(getHighAddress()).append("]\n");
            str.append("# Addresses:\t[").append(getAddressCount()).append("]\n");
            return str.toString();
        }
    }

    public final Calculate getInfo() { return new Calculate(); }

    /*
     * Método principal que realiza os calculos do IP.
     */
    private void calculate(String mask) {
        Matcher matcher = cidrPattern.matcher(mask);

        if (matcher.matches()) {
            address = matchAddress(matcher);

            /* Calcula máscara em binario */
            int cidrPart = rangeCheck(Integer.parseInt(matcher.group(5)), 0, NBITS);
            for (int j = 0; j < cidrPart; ++j) {
                netmask |= (1 << 31 - j);
            }

            /* Calcula endereço de rede em binário */
            network = (address & netmask);

            /* Calcula endereço de broadcast em binário */
            broadcast = network | ~(netmask);
        } else {
            throw new IllegalArgumentException("Máscara incorreta [" + mask + "]");
        }
    }

    private int toInteger(String address) {
        Matcher matcher = addressPattern.matcher(address);
        if (matcher.matches()) {
            return matchAddress(matcher);
        } else {
            throw new IllegalArgumentException("Endereço IP incorreto [" + address + "]");
        }
    }

    /*
     * Método realiza a conversão do endereço IP para inteiro em binário
     */
    private int matchAddress(Matcher matcher) {
        int addr = 0;
        for (int i = 1; i <= 4; ++i) {
            int n = (rangeCheck(Integer.parseInt(matcher.group(i)), 0, 255));
            addr |= ((n & 0xff) << 8*(4-i));
        }
        return addr;
    }

    /*
     * Converte inteiro binário para array de inteiro
     */
    private int[] toArray(int val) {
        int ret[] = new int[4];
        for (int j = 3; j >= 0; --j) {
            ret[j] |= ((val >>> 8*(3-j)) & (0xff));
        }
        return ret;
    }

    /*
     * Formata array de inteiro para endereço de IP
     */
    private String format(int[] octets) {
        StringBuilder str = new StringBuilder();
        for (int i =0; i < octets.length; ++i){
            str.append(octets[i]);
            if (i != octets.length - 1) {
                str.append(".");
            }
        }
        return str.toString();
    }

    private int rangeCheck(int value, int begin, int end) {
        if (value >= begin && value <= end) {
            return value;
        }

        throw new IllegalArgumentException("Valor [" + value + "] está fora do tamanho máximo ["+begin+","+end+"]");
    }

    int pop(int x) {
        x = x - ((x >>> 1) & 0x55555555);
        x = (x & 0x33333333) + ((x >>> 2) & 0x33333333);
        x = (x + (x >>> 4)) & 0x0F0F0F0F;
        x = x + (x >>> 8);
        x = x + (x >>> 16);
        return x & 0x0000003F;
    }

    private String toCidrNotation(String addr, String mask) {
        return addr + "/" + pop(toInteger(mask));
    }
}