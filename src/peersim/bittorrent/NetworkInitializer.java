/*
 * Copyright (c) 2007-2008 Fabrizio Frioli, Michele Pedrolli
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * --
 *
 * Please send your questions/suggestions to:
 * {fabrizio.frioli, michele.pedrolli} at studenti dot unitn dot it
 *
 */

package peersim.bittorrent;

import peersim.core.*;
import peersim.config.Configuration;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;
import java.util.Random;

/**
 * This {@link Control} ...
 */
public class NetworkInitializer implements Control {
	/**
	* The protocol to operate on.
	*
	* @config
	*/
	private static final String PAR_PROT="protocol";

	private static final String PAR_TRANSPORT="transport";

	private static final int TRACKER = 11;

	private static final int CHOKE_TIME = 13;

	private static final int OPTUNCHK_TIME = 14;

	private static final int ANTISNUB_TIME = 15;

	private static final int CHECKALIVE_TIME = 16;

	private static final int TRACKERALIVE_TIME = 17;

	/**
	 *	The distribution of the nodetypes
	 *	@config
	 */
	private static final String NO0="nodetype0";
    private static final String NO1="nodetype1";
	private static final String NO2="nodetype2";
    private static final String NO3="nodetype3";

    /**
	 *	Stores the probability of obtaining different types of nodes
	 */
	public int nodeType0;
    public int nodeType1;
    public int nodeType2;
    public int nodeType3;

	/** Protocol identifier, obtained from config property */
	private final int pid;
	private final int tid;
	private NodeInitializer init;

	private Random rnd;

	public NetworkInitializer(String prefix) {
		pid = Configuration.getPid(prefix+"."+PAR_PROT);
		tid = Configuration.getPid(prefix+"."+PAR_TRANSPORT);
        nodeType0 = (int)(Configuration.getInt(prefix+"."+NO0));
        nodeType1 = (int)Configuration.getInt(prefix+"."+NO1);
        nodeType2 = (int)Configuration.getInt(prefix+"."+NO2);
        nodeType3 = (int)(Configuration.getInt(prefix+"."+NO3));
		init = new NodeInitializer(prefix);
	}

	public boolean execute() {
		int completed;
		Node tracker = Network.get(0);

		// manca l'inizializzazione del tracker;

		((BitTorrent)Network.get(0).getProtocol(pid)).initializeTracker();

		for(int i=1; i<Network.size(); i++){
			//System.err.println("chiamate ad addNeighbor " + i);
			((BitTorrent)Network.get(0).getProtocol(pid)).addNeighbor(Network.get(i));
            int value;
			value = typeSelector(nodeType0,nodeType1,nodeType2,nodeType3);
			boolean valueMobile = mobileSelector();
			init.initialize(Network.get(i),value,valueMobile);
		}
		for(int i=1; i< Network.size(); i++){
			Node n = Network.get(i);
			long latency = ((Transport)n.getProtocol(tid)).getLatency(n,tracker);
			Object ev = new SimpleMsg(TRACKER, n);
			EDSimulator.add(latency,ev,tracker,pid);
			ev = new SimpleEvent(CHOKE_TIME);
			EDSimulator.add(10000,ev,n,pid);
			ev = new SimpleEvent(OPTUNCHK_TIME);
			EDSimulator.add(30000,ev,n,pid);
			ev = new SimpleEvent(ANTISNUB_TIME);
			EDSimulator.add(60000,ev,n,pid);
			ev = new SimpleEvent(CHECKALIVE_TIME);
			EDSimulator.add(120000,ev,n,pid);
			ev = new SimpleEvent(TRACKERALIVE_TIME);
			EDSimulator.add(1800000,ev,n,pid);
		}
		return true;
	}

	private boolean mobileSelector(){
		Random ran = CommonState.r;
		int value = ran.nextInt(100);
		if(value>=50) { return true; }
		else { return false; }
	}

	private int typeSelector(int n0, int n1, int n2, int n3) {
		Random ran = CommonState.r;
		int value = ran.nextInt(n0+n1+n2+n3);
		if(value>=n0){ 					//10%
			if (value >= n0+n1) { 		//20%
				if(value >= n0+n1+n2){ 		//30%
					if(value > n0+n1+n2+n3){  //40%
						return 0;
					}else{
						return 3;
					}
				}else{
					return 2;
				}
			}else{
				return 1;
			}
		}else{
			return 0;
		}

	}

	}
