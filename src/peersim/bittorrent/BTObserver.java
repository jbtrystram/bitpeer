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
import peersim.util.*;

/**
 * This {@link Control} provides a way to keep track of some
 * parameters of the BitTorrent network.
 */
 public class BTObserver implements Control {

	/**
	 *	The protocol to operate on.
	 *	@config
	 */
	private static final String PAR_PROT="protocol";

	/**
	 *	Protocol identifier, obtained from config property
	 */
	private final int pid;

	/**
	 *	The basic constructor that reads the configuration file.
	 *	@param prefix the configuration prefix for this class
	 */
	public BTObserver(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
	}

	/**
	 * Prints information about the BitTorrent network
	 * and the number of leechers and seeders.
	 * Please refer to the code comments for more details.
	 * @return always false
	 */
	public boolean execute() {
		IncrementalFreq nodeStatusStats = new IncrementalFreq();
		IncrementalStats neighborStats = new IncrementalStats();

		int numberOfNodes = Network.size();
		int numberOfCompletedPieces = 0;

		// cycles from 1, since the node 0 is the tracker
		for (int i=1; i<numberOfNodes; ++i) {

			// stats on number of leechers and seeders in the network
			// and consequently also on number of completed files in the network
			nodeStatusStats.add(((BitTorrent)(Network.get(i).getProtocol(pid))).getPeerStatus());

			// stats on number of neighbors per peer
			neighborStats.add(((BitTorrent)(Network.get(i).getProtocol(pid))).getNNodes());
		}

		// number of the pieces of the file, equal for every node, here 1 is chosen,
		// since 1 is the first "normal" node (0 is the tracker)
		int numberOfPieces = ((BitTorrent)(Network.get(1).getProtocol(pid))).nPieces;

		for (int i=1; i<numberOfNodes; ++i) {
			numberOfCompletedPieces = 0;

			// discovers the status of the current peer (leecher or seeder)
			int ps = ((BitTorrent)(Network.get(i).getProtocol(pid))).getPeerStatus();
			String peerStatus;
			if (ps==0) {
				peerStatus = "L"; //leecher
			}
			else {
				peerStatus = "S"; //seeder
			}


			if (Network.get(i)!=null) {

				// counts the number of completed pieces for the i-th node
				for (int j=0; j<numberOfPieces; j++) {
					if ( ((BitTorrent)(Network.get(i).getProtocol(pid))).getFileStatus()[j] == 16) {
						numberOfCompletedPieces++;
					}
				}

				/*
				 * Put here the output lines of the Observer. An example is provided with
				 * basic information and stats.
				 * CommonState.getTime() is used to print out time references
				 * (useful for graph plotting).
				 */
				//Sytem.out de Vincent
        //System.out.println(((BitTorrent)(Network.get(i).getProtocol(pid))).getThisNodeID()+" "+numberOfCompletedPieces);
        //System.out.println(CommonState.getTime()/1000+" "+((BitTorrent)(Network.get(i).getProtocol(pid))).getThisNodeID()+" "+numberOfCompletedPieces);
        //System.out.println("("+((BitTorrent)(Network.get(i).getProtocol(pid))).getThisNodeCoordX()+","+((BitTorrent)(Network.get(i).getProtocol(pid))).getThisNodeCoordY()+")");

        //Affichage MAP 2D
        int mypeerStatus = ((BitTorrent)(Network.get(i).getProtocol(pid))).getPeerStatus();
        mypeerStatus = Network.get(i).getFailState() == Fallible.DOWN ? 2 : mypeerStatus;
		int peerFailState = Network.get(i).getFailState();
        // Target output : Time  ID  coord.X coord.Y pieces L/S Energy-state
        // Sample output : 1000  27   984      153    266    1    1
		//												1 is seeder
				//											  Energy OK = 0, DEAD = 1, DOWN = 2
        System.out.println(
        		CommonState.getTime() + " " +
			Network.get(i).getID() + " " +
        	((BitTorrent)(Network.get(i).getProtocol(pid))).getThisNodeCoordX() + " " +
         	((BitTorrent)(Network.get(i).getProtocol(pid))).getThisNodeCoordY() + " " +
			numberOfCompletedPieces +" "+
						peerStatus + " " +
						mypeerStatus);

        ////By vincent System.out.println("OBS: node " + ((BitTorrent)(Network.get(i).getProtocol(pid))).getThisNodeID() + "(" + peerStatus + ")" + "\t pieces completed: " + numberOfCompletedPieces + "\t \t down: " + ((BitTorrent)(Network.get(i).getProtocol(pid))).nPiecesDown + "\t up: " + ((BitTorrent)(Network.get(i).getProtocol(pid))).nPiecesUp + " time: " + CommonState.getTime());
				////By vincent System.out.println("[OBS] t " + CommonState.getTime() + "\t pc " + numberOfCompletedPieces + "\t n " + ((BitTorrent)(Network.get(i).getProtocol(pid))).getThisNodeID());
			}
			else {
        System.out.println("Node nul");
				////By vincent System.out.println("[OBS] t " + CommonState.getTime() + "\t pc " + "0" + "\t n " + "0");
			}

		}
    System.out.println("");
    System.out.println("");
		// prints the frequency of 0 (leechers) and 1 (seeders)
    //edit vincent
		//nodeStatusStats.printAll(//By vincent System.out);

		// prints the average number of neighbors per peer
    //edit by vincent
		////By vincent System.out.println("Avg number of neighbors per peer: " + neighborStats.getAverage());

		return false;
	}
}
