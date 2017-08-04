/*
 * Main.java
 *
 * Copyright (C) 2017  Pavel Prokhorov (pavelvpster@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.interactiverobotics.vbox_web_client;

import org.virtualbox_5_1.IMachine;
import org.virtualbox_5_1.IProgress;
import org.virtualbox_5_1.ISession;
import org.virtualbox_5_1.IVirtualBox;
import org.virtualbox_5_1.SessionState;
import org.virtualbox_5_1.VirtualBoxManager;

/**
 * Main application class.
 *
 * @author pavelvpster
 */
public final class Main {

    private static final String OK = "OK";
    private static final String ERROR = "ERROR";


    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        if (args.length < 2) {
            System.err.println("Not enough arguments!");
            return;
        }
        final String connectionString = args[0];
        final String command = args[1];
        String parameter = null;
        if (args.length > 2) {
            parameter = args[2];
        }

        final VirtualBoxManager manager = VirtualBoxManager.createInstance(null);
        try {
            manager.connect(connectionString, "", "");
            switch (command) {
                case "list":
                    list(manager.getVBox());
                    break;
                case "list-running":
                    listRunning(manager.getVBox());
                    break;
                case "run":
                    run(manager, manager.getVBox(), parameter);
                    break;
                default:
                    System.err.println("Unsupported command '" + command + "'");
            }
        } finally {
            manager.cleanup();
        }
    }


    private static void list(final IVirtualBox vbox) {
        vbox.getMachines().forEach(Main::printMachineName);
    }

    private static void listRunning(final IVirtualBox vbox) {
        vbox.getMachines().stream()
                .filter(Main::isMachineRunning)
                .forEach(Main::printMachineName);
    }

    private static void printMachineName(final IMachine machine) {
        System.out.println(machine.getName());
    }

    private static boolean isMachineRunning(final IMachine machine) {
        return !SessionState.Unlocked.equals(machine.getSessionState());
    }

    private static void run(final VirtualBoxManager manager, final IVirtualBox vbox, final String machineName) {
        if (machineName == null || machineName.isEmpty()) {
            System.out.println(ERROR);
            return;
        }

        final IMachine machine = vbox.findMachine(machineName);
        if (machine == null) {
            System.out.println(ERROR);
            return;
        }

        if (isMachineRunning(machine)) {
            System.out.println(OK);
            return;
        }

        final ISession session = manager.getSessionObject();
        final IProgress progress = machine.launchVMProcess(session, "gui", "");
        progress.waitForCompletion(-1);
        session.unlockMachine();

        System.out.println(OK);
    }
}
