package claw;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Supplier;

import claw.internal.Config;
import claw.internal.Registry;
import claw.internal.SystemConfigRobot;
import claw.internal.Config.ConfigField;
import claw.api.devices.Device;
import claw.api.CLAWLogger;
import claw.internal.rct.remote.RCTServer;
import claw.api.subsystems.SubsystemCLAW;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

public class CLAWRobot {
    
    // Logs
    
    private static final CLAWLogger
        COMMANDS_LOG = CLAWLogger.getSysLog("Commands"),
        CONFIG_LOG = CLAWLogger.getSysLog("Config"),
        DEVICE_REGISTRY_LOG = CLAWLogger.getSysLog("DeviceRegistry"),
        ROBOT_LOG = CLAWLogger.getSysLog("Robot"),
        SUBSYSTEM_REGISTRY_LOG = CLAWLogger.getSysLog("SubsystemRegistry"),
        SERVER_LOG = CLAWLogger.getSysLog("Server");
    
    // Config
    
    private static final File CONFIG_FILE = new File("/home/lvuser/claw-config.ser");
    private static final Config CONFIG = new Config(CONFIG_LOG, CONFIG_FILE);
    
    private static final ConfigField<String> UNCAUGHT_EXCEPTION_FIELD = CONFIG.getField("UNCAUGHT_EXCEPTION");
    private static final ConfigField<RobotMode> ROBOT_MODE_FIELD = CONFIG.getField("ROBOT_MODE");
    
    
    // CLAWRobot singleton initialization with fromRobot (called in Main.java) and retrieval with getInstance
    
    private static CLAWRobot instance = null;
    
    public static CLAWRobot getInstance () {
        // This runtime exception should never ever happen, as there is practically zero possibility
        // of anyone trying to access a CLAWRobot instance before it is initialized by the
        // fromRobot supplier sent to RobotBase.startRobot in Main.java.
        
        // The only case in which this can reasonably happen is if the CLAWRobot entry point
        // has not yet been added to Main.java
        if (instance == null)
            throw new RuntimeException("CLAWRobot has not been initialized in Main.java");
        return instance;
    }
    
    /**
     * Get a {@code Supplier<RobotBase>} that provides a {@link RobotBase} proxy which CLAW can use. This robot proxy
     * will also start all necessary CLAW processes. This method should only be used in {@code Main.java} as a wrapper
     * around {@code Robot::new}.
     * @param robotSupplier A {@code Supplier<TimedRobot>} which can be used to get a new robot object.
     * @return              The {@code Supplier<RobotBase>} containing the CLAW robot proxy.
     */
    public static Supplier<RobotBase> fromRobot (Supplier<TimedRobot> robotSupplier) {
        return new Supplier<RobotBase>(){
            @Override
            public RobotBase get () {
                
                // When this supplier is called in RobotBase.startRobot, it will initialize the CLAWRobot instance
                if (instance == null)
                    instance = new CLAWRobot(robotSupplier);
                return instance.robotProxy;
            }
        };
    }
    
    
    
    // CLAWRobot private methods
    
    private final Registry<SubsystemCLAW> subsystemRegistry = new Registry<>("subsystem", SUBSYSTEM_REGISTRY_LOG);
    private final Registry<Device<Object>> deviceRegistry = new Registry<>("device", DEVICE_REGISTRY_LOG);
    private final RobotProxy robotProxy;
    private RCTServer server;
    
    private final RobotMode robotMode;
    
    private CLAWRobot (Supplier<TimedRobot> robotSupplier) {
        // Put a message into the console indicating that the CLAWRobot runtime has started
        System.out.println("\n -- CLAWRobot is running -- \n");
        
        // Get the robot initialization mode
        robotMode = ROBOT_MODE_FIELD.getValue(RobotMode.DEFAULT);
        
        // Initialize the robot proxy
        robotProxy = new RobotProxy(robotSupplier);
        
        // Default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(this::handleUncaughtException);
        
        // Send the last uncaught exception
        String uncaughtException = UNCAUGHT_EXCEPTION_FIELD.getValue(null);
        UNCAUGHT_EXCEPTION_FIELD.setValue(null);
        if (uncaughtException != null)
            ROBOT_LOG.err("Uncaught exception from last execution:\n" + uncaughtException);
        
        // Start the RCT server in another thread (so that the server startup is non-blocking)
        new Thread(() -> {
            try {
                server = new RCTServer(5800, SERVER_LOG, subsystemRegistry);
                server.start();
            } catch (IOException e) {
                System.err.println("Failed to start RCT server.");
                e.printStackTrace();
            }
        }).start();
    }
    
    private String getStackTrace (Throwable e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
    
    private void handleUncaughtException (Thread thread, Throwable e) {
        // Print to the driver station
        System.err.println("Caught an uncaught exception: " + e.getMessage());
        
        // Put to the logger
        ROBOT_LOG.err("Uncaught exception in a thread '"+thread.getName()+"':\n"+getStackTrace(e));
    }
    
    private void handleFatalUncaughtException (Throwable e) {
        // Try printing to the driver station
        System.err.println("Caught a fatal uncaught exception: " + e.getMessage());
        
        // Put the stack trace to the uncaught exception field
        UNCAUGHT_EXCEPTION_FIELD.setValue(getStackTrace(e));
    }
    
    /**
     * The robot proxy schedules this method to be called at the default TimedRobot period
     */
    private void robotPeriodic () {
        if (server != null)
            CLAWLogger.sendData(server);
    }
    
    /**
     * The robot proxy calls this method before the startCompetition method exits (regardless
     * of whether any exceptions have been thrown) so that important operations can be finished.
     */
    private void onRobotProgramExit () {
        CONFIG.save();
        ROBOT_LOG.out("Exiting robot program");
        CLAWLogger.sendData(server);
    }
    
    private void onCommandInitialize (Command command) {
        COMMANDS_LOG.out(command.getName() + " initialized");
    }
    
    private void onCommandExecute (Command command) {
        
    }
    
    private void onCommandFinish (Command command) {
        COMMANDS_LOG.out(command.getName() + " finished");
    }
    
    private void onCommandInterrupt (Command command) {
        COMMANDS_LOG.out(command.getName() + " was interrupted");
    }
    
    private void robotSysconfigMode () {
        System.out.println("BOOTING ROBOT IN SYSCONFIG MODE");
    }
    
    
    
    // Public API
    
    public void addSubsystem (SubsystemCLAW subsystem) {
        subsystemRegistry.add(subsystem.getName(), subsystem);
    }
    
    public void addDevice (Device<Object> device) {
        deviceRegistry.add(device.getName(), device);
    }
    
    public void restartCode () {
        restartCode(RobotMode.DEFAULT);
    }
    
    public void restartCode (RobotMode mode) {
        ROBOT_MODE_FIELD.setValue(mode);
        onRobotProgramExit();
        System.exit(0);
    }
    
    
    
    // RobotProxy instance methods
    
    private class RobotProxy extends RobotBase {
        
        private final TimedRobot robot;
        
        public RobotProxy (Supplier<TimedRobot> robotSupplier) {
            // Robot mode handling
            ROBOT_LOG.out("Robot code starting in "+robotMode.name()+" mode");
            
            // Always reset the robot mode to start in DEFAULT next reboot
            ROBOT_MODE_FIELD.setValue(RobotMode.DEFAULT);
            
            // Get the robot to use based on the robotMode
            if (robotMode == RobotMode.SYSCONFIG)
                robot = new SystemConfigRobot(deviceRegistry);
            else
                robot = robotSupplier.get();
            
            // Schedule the robotPeriodic method to be called at the default TimedRobot period
            robot.addPeriodic(CLAWRobot.this::robotPeriodic, TimedRobot.kDefaultPeriod);
            
            CommandScheduler.getInstance().onCommandInitialize(CLAWRobot.this::onCommandInitialize);
            CommandScheduler.getInstance().onCommandExecute(CLAWRobot.this::onCommandExecute);
            CommandScheduler.getInstance().onCommandFinish(CLAWRobot.this::onCommandFinish);
            CommandScheduler.getInstance().onCommandInterrupt(CLAWRobot.this::onCommandInterrupt);
        }
        
        @Override
        public void startCompetition () {
            try {
                
                
                // Get the current robot mode and start the robot based on it
                if (robotMode == RobotMode.SYSCONFIG) robotSysconfigMode();
                else robot.startCompetition();
                
                // Handle robot program exiting
                onRobotProgramExit();
            } catch (Throwable throwable) {
                handleFatalUncaughtException(throwable);
                onRobotProgramExit();
                throw throwable;
            }
        }
        
        @Override
        public void endCompetition () {
            robot.endCompetition();
        }
        
    }
    
    public enum RobotMode {
        DEFAULT,
        SYSCONFIG,
    }
    
}
