package com.gmmapowell.quickbuild.config;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.zinutils.exceptions.UtilException;

import com.gmmapowell.parser.NoChildCommand;
import com.gmmapowell.parser.TokenizedLine;
import com.gmmapowell.utils.ArgumentDefinition;
import com.gmmapowell.utils.Cardinality;

public class NetmaskCommand extends NoChildCommand implements ConfigApplyCommand  {
	private String name;
	private String ip;
	private int bits;
	
	public NetmaskCommand(TokenizedLine toks) {
		toks.process(this, 
				new ArgumentDefinition("*", Cardinality.REQUIRED, "name", "property name"),
				new ArgumentDefinition("*", Cardinality.REQUIRED, "ip", "ip address in range"),
				new ArgumentDefinition("*", Cardinality.REQUIRED, "bits", "number of significant bits")
		);
	}

	@Override
	public void applyTo(Config config) {
		int nbytes = bits/8;
		int thenbits = bits%8;
		boolean applies = false;
		try {
			InetAddress addr = InetAddress.getByName(ip);
			for (InetAddress l : localAddresses()) {
				if (l instanceof Inet4Address) {
					if (netmask(addr.getAddress(), l.getAddress(), nbytes, thenbits)) {
						applies = true;
						break;
					}
				}
			}
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
		config.setVarProperty(name, Boolean.toString(applies));
	}

	private boolean netmask(byte[] want, byte[] have, int nbytes, int thenbits) {
		if (want.length != 4 || have.length != 4)
			return false;
		for (int i=0;i<nbytes;i++) {
			if (want[i] != have[i])
				return false;
		}
		for (int i=0;i<thenbits;i++) {
			int mask = 1<<(7-i);
			if ((want[nbytes]&mask) != (have[nbytes]&mask))
				return false;
		}
		return true;
	}

	private Iterable<InetAddress> localAddresses() throws SocketException {
		List<InetAddress> ret = new ArrayList<>();
		Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
		while(e.hasMoreElements())
		{
		    NetworkInterface n = (NetworkInterface) e.nextElement();
		    Enumeration<InetAddress> ee = n.getInetAddresses();
		    while (ee.hasMoreElements())
		        ret.add((InetAddress) ee.nextElement());
		}
		return ret;
	}
}
