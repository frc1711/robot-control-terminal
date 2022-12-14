package frc.robot.commands;

import claw.logs.LogHandler;
import claw.logs.RCTLog;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.TestSubsystem;

public class TestCommand extends CommandBase {
    
    private final static RCTLog LOG = LogHandler.getInstance().getLog("TestCommand");
    private final TestSubsystem subsystem;
    
    public TestCommand (TestSubsystem subsystem) {
        this.subsystem = subsystem;
        addRequirements(subsystem);
        LOG.out("Constructing TestCommand");
    }
    
    @Override
    public void initSendable (SendableBuilder builder) {
        builder.addStringProperty("command key name", () -> "command string value", str -> {});
    }
    
    @Override
    public void initialize () {
        LOG.out("Initializing TestCommand");
        subsystem.stop();
    }
    
    @Override
    public void execute () {
        subsystem.set(0.4);
    }
    
    @Override
    public void end (boolean interrupted) {
        LOG.out("Ending TestCommand");
        subsystem.stop();
    }
    
    @Override
    public boolean isFinished () {
        return false;
    }
    
}
