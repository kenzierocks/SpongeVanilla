package org.granitemc.granite.utils;

import org.granitemc.granite.Mappings;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.Objects;

public class ServerComposite {
    static Class<?> serverClass = null;
    static Class<?> commandHandlerClass = null;
    static Object server = null;
    static Object commandHandler = null;

    static boolean commandProxyInstalled = false;

    public static void create(String[] args) {
        //attempt to locate the MinecraftServer class
        serverClass = Mappings.getClassByHumanName("net.minecraft.server.dedicated.DedicatedServer");

        //start the server
        Mappings.call(null, "net.minecraft.init.Bootstrap", "func_151354_b");

        String worldsDirectory = ".";
        for (int argIndex = 0; argIndex < args.length; ++argIndex) {
            String argument = args[argIndex];
            String argumentValue = argIndex == args.length - 1 ? null : args[argIndex + 1];
            boolean hasValue = false;

            if (!argument.equals("nogui") && !argument.equals("--nogui")) {
                if (argument.equals("--port") && argumentValue != null) {
                    hasValue = true;
                    try {
                        Integer.parseInt(argumentValue);
                    } catch (NumberFormatException ignored) {

                    }
                } else if (argument.equals("--singleplayer") && argumentValue != null) {
                    hasValue = true;
                } else if (argument.equals("--universe") && argumentValue != null) {
                    hasValue = true;
                    worldsDirectory = argumentValue;
                } else if (argument.equals("--world") && argumentValue != null) {
                    hasValue = true;
                } else if (argument.equals("--demo")) {

                } else if (argument.equals("--bonusChest")) {

                }
            } else {

            }

            if (hasValue) ++argIndex;
        }

        //creating proxy

        //commandproxy first

        commandHandlerClass = Mappings.getClassByHumanName("net.minecraft.command.ServerCommandHandler");
        //proxy out the ServerCommandManager

        MethodHandler serverCommandHandler = new MethodHandler() {
            @Override
            public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) {
                Logger.info("Command proxy triggered!");
                Logger.info(thisMethod.getName() + " ->" + proceed.getName());
                return null;
                /*
                try {
                    if(Objects.equals(thisMethod.getDeclaredName(), "a") && args.length >= 5){
                        //commands are processed here:
                        //args: ae var1, ac var2, int var3, String var4, Object ... var5
                        //ae is castable to player
                        boolean cancelVanillaCommand = false;
                        String[] commandParams = ((String)args[3]).split(" ");
                        Logger.info("intercepted command: " + commandParams[0]);

                        if(!cancelVanillaCommand) proceed.invoke(self, args);
                    } else {
                        return proceed.invoke(self, args);
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    Logger.error("Failed to invoke " + proceed);
                    e.printStackTrace();
                }
                return null;
                */
            }
        };

        MethodHandler serverHandler = new MethodHandler() {
            @Override
            public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) {
                try {
                    if (!commandProxyInstalled){
                        try {
                            commandProxyInstalled = true;
                            Field commandManager = self.getClass().getSuperclass().getSuperclass().getDeclaredField("p");
                            commandManager.setAccessible(true);
                            Logger.info("Gained access to server command manager.");
                            commandManager.set(self, commandHandler);
                        } catch (NoSuchFieldException e) {
                            Logger.error("Unable to modify command manager field.");
                            e.printStackTrace();
                        }
                    }

                    return proceed.invoke(self, args);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    Logger.error("Failed to invoke " + proceed);
                    e.printStackTrace();
                }
                return null;
            }
        };

        try {
            ProxyFactory factory = new ProxyFactory();
            factory.setSuperclass(serverClass);
            server = factory.create(new Class[]{File.class}, new Object[]{new File(worldsDirectory)}, serverHandler);
        } catch (NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            Logger.error("Failed to instanciate server.");
            e.printStackTrace();
            return;
        }
        try {
            ProxyFactory factory = new ProxyFactory();
            factory.setSuperclass(commandHandlerClass);
            commandHandler = factory.create(new Class[]{}, new Object[]{});
            ((Proxy) commandHandler).setHandler(serverCommandHandler);
        } catch (NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            Logger.error("Failed to create command proxy.");
            e.printStackTrace();
            return;
        }

        //these will need to be refactored to the obfuscated variants
        //if (serverOwner != null) serverProxy.setServerOwner(serverOwner);
        //if (world != null) serverProxy.setFolderName(world);
        //if (serverPort >= 0) serverProxy.setServerPort(serverPort);
        //if (demo) serverProxy.setDemo(true);
        //if (bonusChest) serverProxy.canCreateBonusChest(true);
        //if (showGui && !GraphicsEnvironment.isHeadless()) serverProxy.setGuiEnabled();     

        // fire up the server thread
        try {
            Mappings.call(server,"net.minecraft.server.dedicated.DedicatedServer", "startServerThread");
        } catch (IllegalArgumentException | SecurityException | NullPointerException e) {
            Logger.error("Failed to start server thread.");
            e.printStackTrace();
        }

        //attempt to locate the ThreadServerShutdown class
        Object serverShutdownThread;
        try {
            serverShutdownThread = Mappings.getClassByHumanName("net.minecraft.server.ThreadServerShutdown").getDeclaredConstructor(String.class, serverClass).newInstance("Server Shutdown Thread", server);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            Logger.error("Failed to load server shutdown thread class.");
            e.printStackTrace();
            return;
        }

        //add shutdown hook
        Runtime.getRuntime().addShutdownHook((Thread) serverShutdownThread);

        //end minecraft main
    }
}
