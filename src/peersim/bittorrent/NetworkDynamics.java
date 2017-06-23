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

import peersim.config.*;
import peersim.core.*;
import peersim.transport.*;
import peersim.edsim.*;
import java.util.Random;

/**
 *	This {@link Control} can change the size of networks by adding and removing
 *	nodes. This class supports only permanent removal of nodes
 *	and the addition of brand new nodes. That is, temporary downtime
 *	is not supported by this class.
 */
public class NetworkDynamics implements Control {
	private static final int TRACKER = 11;
	private static final int CHOKE_TIME = 13;
	private static final int OPTUNCHK_TIME = 14;
	private static final int ANTISNUB_TIME = 15;
	private static final int CHECKALIVE_TIME = 16;
	private static final int TRACKERALIVE_TIME = 17;

	/**
	 *	The protocol to operate on.
	 *	@config
	 */
	private static final String PAR_PROT="protocol";

	/**
	 * Nodes are removed until the size specified by this parameter is reached. The
	 * network will never go below this size as a result of this class.
	 * Defaults to 0.
	 * @config
	 */
	private static final String PAR_MIN = "minsize";

	/**
	 * Specifies if the tracker can disappear from the network.
	 * 0 means no, 1 means yes
	 * @config
	 */
	private static final String PAR_TRACKER_DIE ="tracker_can_die";

	/**
	 * The Transport used by the the control.
	 * @config
	 */
	private static final String PAR_TRANSPORT="transport";

	/**
     * Specifies how many nodes will be added to the network.
	 * @config
	 */
	private static final String PAR_ADD="add";

	/**
	 * Specifies how many nodes will be removed from the network.
	 * @config
	 */
	private static final String PAR_REMOVE="remove";


	/*
	 *	The following are local variables, obtained from config property.
	 */
	private final int pid;
	private final int tid;
	private final int maxSize;
	private final int minsize;
	private boolean trackerCanDie = false; // false (value 0) by default
	private final int add; // number of nodes to be added
	private final int remove; // number of nodes to be removed

	private final NodeInitializer init;
	private Node tracker;

	/**
	 * Standard constructor that reads the configuration parameters.
	 * Invoked by the simulation engine.
	 * @param prefix the configuration prefix for this class
	 */
	public NetworkDynamics(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		minsize = Configuration.getInt(prefix + "." + PAR_MIN, 0);
		tid = Configuration.getPid(prefix+"."+PAR_TRANSPORT);
		add = Configuration.getInt(prefix + "." + PAR_ADD);
		remove = Configuration.getInt(prefix + "." + PAR_REMOVE);

		/*
		 * By default, the tracker can not disappear.
		 * If control.dynamics.tracker_can_die is set to 1, the tracker can die.
		 */
		if (Configuration.getInt(prefix + "." + PAR_TRACKER_DIE) == 1) {
			trackerCanDie = true;
		}

		init = new NodeInitializer("init.net");
		tracker = Network.get(0);

		maxSize = (Network.size()-1) + ((BitTorrent)tracker.getProtocol(pid)).maxGrowth;
	}


	/**
	 * Adds n nodes to the network.
	 * New nodes can be added only if the tracker is up.
	 *
	 * @param n the number of nodes to add, must be non-negative.
	 */
	protected void add(int n) {
		if (n==0)
			return;
		// tracker is up
		if (tracker.isUp()) {
			for (int i = 0; i < n; ++i) {
				// create a new node
				Node nodeToBeAdded = (Node) Network.prototype.clone();

				// add the new node to the network
				Network.add(nodeToBeAdded); // questo nodo sara' in posizione Network.len -1

				/*
				 * Initialize the new node using the NodeInitializer class;
				 * this it the same as init.initialize(Network.get(Network.size()-1));
				 */
				init.initialize(nodeToBeAdded, 2, false);

				/*
				 * The new node sends a TRACKER message to the tracker, asking for
				 * a list of peers. The tracker will respond with a PEERSET message.
				 * All the related events are also attached to the new node.
				 */
				long latency = ((Transport)nodeToBeAdded.getProtocol(tid)).getLatency(nodeToBeAdded,tracker);

				Object ev = new SimpleMsg(TRACKER, nodeToBeAdded);
				EDSimulator.add(latency,ev,tracker,pid);

				ev = new SimpleEvent(CHOKE_TIME);
				EDSimulator.add(10000,ev,nodeToBeAdded,pid);
				ev = new SimpleEvent(OPTUNCHK_TIME);
				EDSimulator.add(30000,ev,nodeToBeAdded,pid);
				ev = new SimpleEvent(ANTISNUB_TIME);
				EDSimulator.add(60000,ev,nodeToBeAdded,pid);
				ev = new SimpleEvent(CHECKALIVE_TIME);
				EDSimulator.add(120000,ev,nodeToBeAdded,pid);
				ev = new SimpleEvent(TRACKERALIVE_TIME);
				EDSimulator.add(1800000,ev,nodeToBeAdded,pid);

				// add the new node to the tracker's cache
				if ( ((BitTorrent)tracker.getProtocol(pid)).addNeighbor(nodeToBeAdded) )
				//Edit vincent
				System.out.print("");
				////By vincent System.out.println("DYN: A new node has been added to the network.");
			}
		}
		/*
		 * Otherwise, the tracker is down and no new nodes
		 * can be added to the network.
		 */
		else
		System.out.print("");
		//Edit vincent
		////By vincent System.out.println("DYN: Tracker is down. No new nodes added.");
	}

	/**
	 * Removes n nodes from the network.
	 * A node can be removed either if the tracker is up or down;
	 * if the tracker is up, the node to be removed will be removed also
	 * from the tracker's cache.
	 *
	 * @param n the number of nodes to remove.
	 */
	protected void remove(int n) {
		// the index of the node to be removed
		int nodeIndex=0;

		for (int i=0; i<n; ++i) {
			nodeIndex = CommonState.r.nextInt(Network.size());
			// if the tracker can not disappear from the network
			if (!trackerCanDie) {
				/*
				 * Choose an index for the node to be removed.
				 * The value 0 will be discarded, since the tracker cannot disappear.
				 * Non existing nodes cannot be removed: if the returned index corresponds
				 * to a non-existing node, a new index will be generated.
				 */
					while (nodeIndex==0) {
						nodeIndex = CommonState.r.nextInt(Network.size());
				}
			}
			// otherwise, also the tracker can disappear
			else {
				nodeIndex = CommonState.r.nextInt(Network.size());
			}

			// a warning message
			//if (nodeIndex==0)
			//	//By vincent System.out.println("DYN: The tracker is going to disapper.");

			// remove the node with the given index from the network
			Node nodeToBeRemoved = Network.remove(nodeIndex);

			// then remove it from the tracker's cache, if it is possible (= the tracker is up);
			if (tracker.isUp()) {
				if ( ((BitTorrent)tracker.getProtocol(pid)).removeNeighbor(nodeToBeRemoved) )
				//Edit vincent
				System.out.print("");
				////By vincent System.out.println("DYN: A node has been removed from the network.");
			}
			else { // the tracker is down
				System.out.print("");
				//Edit vincent
				////By vincent System.out.println("DYN: The tracker is DOWN!");
			}
		}
	}

	//By Vincent 		// the index of the node to move
	protected void move(int nodeIndex){

		int oldY=((BitTorrent)(Network.get(nodeIndex).getProtocol(pid))).getThisNodeCoordY();
		int oldX=((BitTorrent)(Network.get(nodeIndex).getProtocol(pid))).getThisNodeCoordX();
		Random ran = CommonState.r;
		/* --- Random walk --- */
		// int newX = ran.nextInt(10) - 5;
		// int newY = ran.nextInt(10) - 5;

		/* --- Constant speed --- */
		int newX=((BitTorrent)(Network.get(nodeIndex).getProtocol(pid))).getThisNodeSpeedX();
		int newY=((BitTorrent)(Network.get(nodeIndex).getProtocol(pid))).getThisNodeSpeedY();

		if((oldX+newX)>1000){
			newX=-newX;
			Network.node[nodeIndex].setSpeedX(newX);
			((BitTorrent)(Network.get(nodeIndex).getProtocol(pid))).setThisNodeSpeedX(newX);
		}else if((oldX+newX) < 0){
			newX=-newX;
			Network.node[nodeIndex].setSpeedY(newX);
			((BitTorrent)(Network.get(nodeIndex).getProtocol(pid))).setThisNodeSpeedX(newX);

		}

		if((oldY+newY)>1000){
			newY=-newY;
			Network.node[nodeIndex].setSpeedY(newY);
			((BitTorrent)(Network.get(nodeIndex).getProtocol(pid))).setThisNodeSpeedY(newY);

		}else if((oldY+newY) < 0){
			newY=-newY;
			Network.node[nodeIndex].setSpeedY(newY);
			((BitTorrent)(Network.get(nodeIndex).getProtocol(pid))).setThisNodeSpeedY(newY);

		}

		Node nodeToMove = Network.move(nodeIndex,newX,newY);
		if ( ((BitTorrent)tracker.getProtocol(pid)).moveNeighbor(nodeToMove)){
			//nodeIndex = CommonState.r.nextInt(Network.size());
			// then remove it from the tracker's cache, if it is possible (= the tracker is up);
			if (tracker.isUp()) {
				((BitTorrent)(Network.get(nodeIndex).getProtocol(pid))).setThisNodeCoordY(oldY+newY);
				((BitTorrent)(Network.get(nodeIndex).getProtocol(pid))).setThisNodeCoordX(oldX+newX);
			}
		}else{
			System.err.println("erreur moveNeighbor");
		}
			//System.out.print("Coucou, je suis le tracker et j'ai bien vu qu'un noeud a bougé");
			////By vincent System.out.println("DYN: A node has been removed from the network.");
	}
	/**
	 * Calls {@link #add(int)} or {@link #remove} with the parameters defined by the
	 * configuration.
	 * @return always false
	 */
	public boolean execute(){
		int choice = (CommonState.r.nextInt(11)); // 0 or 1 or more

		for (int j=0; j<=Network.size()-1;j++){
			if(((BitTorrent)(Network.get(j).getProtocol(pid))).getNodeMobility()){
				move(j);
			}
		}

		// adding new nodes
		if (choice == 0) {
			/*
			 * If the specified number of nodes cannot be added,
			 * it tries to add a less number of nodes without
			 * going out of bounds. Otherwise, all specified nodes
			 * will be added.
			 */
			if (Network.size() + this.add > maxSize) {
				////By vincent System.out.println("DYN: " + (maxSize - Network.size()) + " nodes will be added.");
				add(maxSize - Network.size());
			}
			else {
				////By vincent System.out.println("DYN: " + this.add + " nodes will be added.");
				add(this.add);
			}
		}
		// removing existing nodes
		else if(choice == 1){
			if (Network.size() - this.remove < minsize) {
				////By vincent System.out.println("DYN: " + (Network.size() - minsize) + " nodes will be removed.");
				remove(Network.size() - minsize);
			}
			else {
				////By vincent System.out.println("DYN: " + this.remove + " nodes will be removed.");
				remove(this.remove);
			}
		} else {
			// find a node and randomly toggle its state between OK and DOWN
			// only switch a node that has energy
			int toggledNode = CommonState.r.nextInt(Network.size());
			if(Network.node[toggledNode] != null
			&& Network.node[toggledNode].getFailState() != Fallible.DEAD
			// && Network.node[toggledNode].getIndicatorEnergy() == 0
			){
				Network.node[toggledNode].setFailState(
					Network.node[toggledNode].getFailState() == Fallible.OK ? Fallible.DOWN : Fallible.OK
				);
			}
		}
		return false;
	}
}
