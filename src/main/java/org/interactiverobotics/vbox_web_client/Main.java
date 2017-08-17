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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        if (args.length == 1 && "ping".equals(args[0])) {
            System.out.println(OK);
            return;
        }

        if (args.length < 2) {
            System.err.println("Not enough arguments!");
            return;
        }
        final String connectionString = args[0];
        final String command = args[1];
        final List<String> parameters = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            parameters.add(args[i]);
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
                    run(manager, manager.getVBox(),
                            getStringParameter(parameters, 0), getLongParameter(parameters, 1));
                    break;
                default:
                    System.err.println("Unsupported command '" + command + "'");
            }
        } catch (final Exception e) {
            System.out.println(ERROR);
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

    private static Optional<String> getStringParameter(final List<String> parameters, int index) {
        if (index < 0 || index >= parameters.size()) {
            return Optional.empty();
        }
        return Optional.of(parameters.get(index));
    }

    private static Optional<Long> getLongParameter(final List<String> parameters, int index) {
        return getStringParameter(parameters, index)
                .flatMap(t -> {
                    try {
                        return Optional.of(Long.parseLong(t));
                    } catch (final NumberFormatException e) {
                        return Optional.empty();
                    }
                });
    }

    private static void run(final VirtualBoxManager manager, final IVirtualBox vbox,
                            final Optional<String> machineName, final Optional<Long> memoryLimit) {
        if (!machineName.isPresent()) {
            System.out.println(ERROR);
            return;
        }

        final IMachine machine = vbox.findMachine(machineName.get());
        if (machine == null) {
            System.out.println(ERROR);
            return;
        }

        if (isMachineRunning(machine)) {
            System.out.println(OK);
            return;
        }

        if (memoryLimit.isPresent()) {
            final long memoryUsed = vbox.getMachines().stream()
                    .filter(Main::isMachineRunning)
                    .map(IMachine::getMemorySize)
                    .mapToLong(Long::longValue)
                    .sum();
            if (memoryUsed + machine.getMemorySize() >= memoryLimit.get()) {
                System.out.println(ERROR);
                return;
            }
        }

        final ISession session = manager.getSessionObject();
        final IProgress progress = machine.launchVMProcess(session, "gui", "");
        progress.waitForCompletion(-1);
        session.unlockMachine();

        System.out.println(OK);
    }
}
