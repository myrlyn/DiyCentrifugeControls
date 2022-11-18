//MIN RCF is ~95
//MAX RCF is ~ 6450

package diycentrifuge;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ServiceLoader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import org.apache.commons.lang3.StringUtils;

import com.pi4j.context.Context;
import com.pi4j.extension.Plugin;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import com.pi4j.io.i2c.I2CProvider;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmConfig;
import com.pi4j.io.pwm.PwmConfigBuilder;
import com.pi4j.io.pwm.PwmType;
import com.pi4j.platform.Platforms;

@SuppressWarnings("unused")
public class DiyCentrifuge {
	private static String banner = "" 
			+ " ██████████    ███                  █████████                       █████               ███     ██████                              \r\n"
			+ "░░███░░░░███  ░░░                  ███░░░░░███                     ░░███               ░░░     ███░░███                             \r\n"
			+ " ░███   ░░███ ████  █████ ████    ███     ░░░   ██████  ████████   ███████   ████████  ████   ░███ ░░░  █████ ████  ███████  ██████ \r\n"
			+ " ░███    ░███░░███ ░░███ ░███    ░███          ███░░███░░███░░███ ░░░███░   ░░███░░███░░███  ███████   ░░███ ░███  ███░░███ ███░░███\r\n"
			+ " ░███    ░███ ░███  ░███ ░███    ░███         ░███████  ░███ ░███   ░███     ░███ ░░░  ░███ ░░░███░     ░███ ░███ ░███ ░███░███████ \r\n"
			+ " ░███    ███  ░███  ░███ ░███    ░░███     ███░███░░░   ░███ ░███   ░███ ███ ░███      ░███   ░███      ░███ ░███ ░███ ░███░███░░░  \r\n"
			+ " ██████████   █████ ░░███████     ░░█████████ ░░██████  ████ █████  ░░█████  █████     █████  █████     ░░████████░░███████░░██████ \r\n"
			+ "░░░░░░░░░░   ░░░░░   ░░░░░███      ░░░░░░░░░   ░░░░░░  ░░░░ ░░░░░    ░░░░░  ░░░░░     ░░░░░  ░░░░░       ░░░░░░░░  ░░░░░███ ░░░░░░  \r\n"
			+ "                     ███ ░███                                                                                      ███ ░███         \r\n"
			+ "                    ░░██████                                                                                      ░░██████          \r\n"
			+ "                     ░░░░░░                                                                                        ░░░░░░           \r\n"
			+ "\r\n"
			+ "Written by Jonathon Walker, Licensed under GPLV3";

	private static final String CONSOLAS = "Consolas";
	private static final double MIN_STARTUP_DUTYCYCLE = 25;
	private static final double MINDUTYCYCLE = 12;
	private static final String DURATION_MINS = "Duration(Mins)";
	protected static DiyCentrifuge window = null;
	private static final String MESSAGES = "Messages";
	private static JFrame frame;
	private static JTextField rcfTextField;
	private static JTextField rpmTextField;
	private static JTextField timeTextField;
	protected static final double DEFAULTMAXRPM = 12000.0;// max rpm of JYCRS390H motor is 12000 RPM@12v, 100% duty
	protected static final double DEFAULTMINRPM = 1458.0;
	protected static final int DEFAULTRADIUS = 40;// radius of rotor is ~ 4cm(40mm)
	protected static final int DEFAULTENBPIN = 19;
	protected static final int DEFAULTIN4PIN = 16;
	protected static final int DEFAULTIN3PIN = 20;
	private static Logger logger = Logger.getLogger("diycentrifuge");
	protected static JTextPane messagesPane = new JTextPane();
	protected static JTabbedPane tabbedPane = new JTabbedPane(javax.swing.SwingConstants.TOP);
	static JCheckBox unlimitedDurationCheckbox = new JCheckBox("Unlimited Duration");
	static Pwm enb;
	static DigitalOutput in3;
	static DigitalOutput in4;
	protected static Timer stopTimer = null;
	static Context ctx = com.pi4j.Pi4J.newAutoContext();
	Platforms platforms = ctx.platforms();
	protected static TimerTask stopTask = new TimerTask() {
		public void run() {
			DiyCentrifuge.enb.off();
		}
	};

	public static void newTimer() {
		newStopTimer();
	}

	public static double rpmFromDesiredRcf(Integer rcf) {

		return 1000 * Math.sqrt(rcf.doubleValue() / (DEFAULTRADIUS * 1.118d));

	}

	public static float dutyCycleFromRpm(double rpm) {
		double dval = rpm / DEFAULTMAXRPM;
		dval = dval * 100;
		float dcyc = (float) dval;
		if (dcyc > 100f) {
			dcyc = 0f;
			String s = messagesPane.getText();
			s += "Requested RCF Too Large.\n";
			int messageindex = tabbedPane.indexOfTab(MESSAGES);
			tabbedPane.setSelectedIndex(messageindex);

		}
		return dcyc;
	}

	static void newRcfField() {
		rcfTextField = new JTextField();
	}

	protected static void newEnb(PwmConfigBuilder p1) {
		enb = ctx.create(p1);
	}

	/**
	 * @wbp.nonvisual location=91,469
	 */

	/**
	 * Launch the application.
	 */

	@SuppressWarnings("static-access")
	public static void main(String[] args) {

		EventQueue.invokeLater(() -> {
			try {

				window = new DiyCentrifuge();
				window.frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}

		});
	}

	/**
	 * Create the application.
	 */
	public DiyCentrifuge() {
		initialize();
	}


	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		setupLogFile();
		logger.log(Level.INFO,"\n{0}",banner);
		newFrame();
		frame.setBounds(0, 0, 480, 320);
		frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		frame.setUndecorated(true);
		frame.setAlwaysOnTop(true);
		frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		logger.info("Testing Spinup");
		ServiceLoader<Plugin> svcld = ServiceLoader.load(Plugin.class);
		pinSetup();
		motorSpinCheck();
		JPanel rcfPanel = new JPanel();
		tabbedPane.addTab("RCF", null, rcfPanel, null);
		rcfPanel.setLayout(new BorderLayout(0, 0));

		JPanel rcfTextPanel = new JPanel();
		rcfPanel.add(rcfTextPanel, BorderLayout.NORTH);
		rcfTextPanel.setLayout(new BorderLayout(0, 0));

		newRcfField();
		rcfTextField.setFont(new Font(CONSOLAS, Font.PLAIN, 16));
		rcfTextPanel.add(rcfTextField);
		rcfTextField.setColumns(24);
		rcfTextField.setEditable(false);
		JPanel rcfButtonPanel = new JPanel();
		rcfPanel.add(rcfButtonPanel, BorderLayout.CENTER);
		rcfButtonPanel.setLayout(new GridLayout(4, 4, 2, 2));

		JButton sevenButton = new JButton("7");
		sevenButton.addActionListener((ActionEvent e) -> {
			String s = rcfTextField.getText();
			s = s + "7";
			rcfTextField.setText(s);

		});
		rcfButtonPanel.add(sevenButton);

		JButton eightButton = new JButton("8");
		eightButton.addActionListener((ActionEvent e) -> {
			String s = rcfTextField.getText();
			s = s + "8";
			rcfTextField.setText(s);

		});
		rcfButtonPanel.add(eightButton);

		JButton nineButton = new JButton("9");
		nineButton.addActionListener((ActionEvent e) -> {
			String s = rcfTextField.getText();
			s = s + "9";
			rcfTextField.setText(s);

		});
		rcfButtonPanel.add(nineButton);

		JButton fourButton = new JButton("4");
		fourButton.addActionListener((ActionEvent e) -> {
			String s = rcfTextField.getText();
			s = s + "4";
			rcfTextField.setText(s);
		});
		rcfButtonPanel.add(fourButton);

		JButton fiveButton = new JButton("5");
		fiveButton.addActionListener((ActionEvent e) -> {
			String s = rcfTextField.getText();
			s = s + "5";
			rcfTextField.setText(s);
		});
		rcfButtonPanel.add(fiveButton);

		JButton sixButton = new JButton("6");
		sixButton.addActionListener((ActionEvent e) -> {
			String s = rcfTextField.getText();
			s = s + "6";
			rcfTextField.setText(s);
		});
		rcfButtonPanel.add(sixButton);

		JButton oneButton = new JButton("1");
		oneButton.addActionListener((ActionEvent e) -> {
			String s = rcfTextField.getText();
			s = s + "1";
			rcfTextField.setText(s);
		});
		rcfButtonPanel.add(oneButton);

		JButton twoButon = new JButton("2");
		twoButon.addActionListener((ActionEvent e) -> {
			String s = rcfTextField.getText();
			s = s + "2";
			rcfTextField.setText(s);
		});
		rcfButtonPanel.add(twoButon);

		JButton threeButton = new JButton("3");
		threeButton.addActionListener((ActionEvent e) -> {
			String s = rcfTextField.getText();
			s = s + "3";
			rcfTextField.setText(s);
		});
		rcfButtonPanel.add(threeButton);

		JButton backspaceButton = new JButton("Bksp");
		backspaceButton.addActionListener((ActionEvent e) -> {
			String s = rcfTextField.getText();
			s = StringUtils.chop(s);
			rcfTextField.setText(s);
		});
		rcfButtonPanel.add(backspaceButton);

		JButton zeroButton = new JButton("0");
		zeroButton.addActionListener((ActionEvent e) -> {
			String s = rcfTextField.getText();
			s = s + "0";
			rcfTextField.setText(s);
		});
		rcfButtonPanel.add(zeroButton);

		JButton enterButton = new JButton("Enter");
		enterButton.addActionListener((ActionEvent e) -> {
			Integer rcftmp = Integer.parseInt(DiyCentrifuge.rcfTextField.getText());
			logger.info("requested RCF is " + rcftmp);
			double rpms = DiyCentrifuge.rpmFromDesiredRcf(rcftmp);
			Long lrpms = Math.round(rpms);
			logger.info("Calculated RPMs: " + lrpms);
			DiyCentrifuge.rpmTextField.setText(lrpms.toString());
			int ind = DiyCentrifuge.tabbedPane.indexOfTab(DURATION_MINS);
			tabbedPane.setSelectedIndex(ind);
		});
		rcfButtonPanel.add(enterButton);

		JPanel rpmPanel = new JPanel();
		tabbedPane.addTab("RPM", null, rpmPanel, null);
		rpmPanel.setLayout(new BorderLayout(0, 0));

		JPanel rpmButtonPanel = new JPanel();
		rpmPanel.add(rpmButtonPanel, BorderLayout.CENTER);
		rpmButtonPanel.setLayout(new GridLayout(4, 3, 0, 0));

		JButton sevenButtonRpm = new JButton("7");
		sevenButtonRpm.addActionListener((ActionEvent e) -> rpmTextField.setText(rpmTextField.getText() + "7"));
		rpmButtonPanel.add(sevenButtonRpm);

		JButton eightButtonRpm = new JButton("8");
		eightButtonRpm.addActionListener((ActionEvent e) -> rpmTextField.setText(rpmTextField.getText() + "8"));
		rpmButtonPanel.add(eightButtonRpm);

		JButton nineButtonRpm = new JButton("9");
		nineButtonRpm.addActionListener((ActionEvent e) -> rpmTextField.setText(rpmTextField.getText() + "9"));
		rpmButtonPanel.add(nineButtonRpm);

		JButton fourButtonRpm = new JButton("4");
		fourButtonRpm.addActionListener((ActionEvent e) -> rpmTextField.setText(rpmTextField.getText() + "4"));
		rpmButtonPanel.add(fourButtonRpm);

		JButton fiveButtonRpm = new JButton("5");
		fiveButtonRpm.addActionListener((ActionEvent e) -> rpmTextField.setText(rpmTextField.getText() + "5"));

		rpmButtonPanel.add(fiveButtonRpm);

		JButton sixButtonRpm = new JButton("6");
		sixButtonRpm.addActionListener((ActionEvent e) -> rpmTextField.setText(rpmTextField.getText() + "6"));

		rpmButtonPanel.add(sixButtonRpm);

		JButton oneButtonRpm = new JButton("1");
		oneButtonRpm.addActionListener((ActionEvent e) -> rpmTextField.setText(rpmTextField.getText() + "1"));

		rpmButtonPanel.add(oneButtonRpm);

		JButton twoButtonRpm = new JButton("2");
		twoButtonRpm.addActionListener((ActionEvent e) -> rpmTextField.setText(rpmTextField.getText() + "2"));

		rpmButtonPanel.add(twoButtonRpm);

		JButton threeButtonRpm = new JButton("3");
		threeButtonRpm.addActionListener((ActionEvent e) -> rpmTextField.setText(rpmTextField.getText() + "3"));

		rpmButtonPanel.add(threeButtonRpm);

		JButton backspaceRpm = new JButton("\tBksp");
		backspaceRpm
				.addActionListener((ActionEvent e) -> rpmTextField.setText(StringUtils.chop(rpmTextField.getText())));
		rpmButtonPanel.add(backspaceRpm);

		JButton zeroButtonRpm = new JButton("0");
		zeroButtonRpm.addActionListener((ActionEvent e) -> rpmTextField.setText(rpmTextField.getText() + "0"));
		rpmButtonPanel.add(zeroButtonRpm);

		JButton enterButtonRpm = new JButton("Enter");
		enterButtonRpm.addActionListener((ActionEvent e) -> {
			int ind = tabbedPane.indexOfTab(DURATION_MINS);
			tabbedPane.setSelectedIndex(ind);
		});
		rpmButtonPanel.add(enterButtonRpm);

		JPanel rpmTextPanel = new JPanel();
		rpmPanel.add(rpmTextPanel, BorderLayout.NORTH);
		rpmTextPanel.setLayout(new BorderLayout(0, 0));

		newRpmTextField();
		rpmTextField.setFont(new Font(CONSOLAS, Font.PLAIN, 14));
		rpmTextPanel.add(rpmTextField);
		rpmTextField.setColumns(34);

		JPanel timePanel = new JPanel();
		tabbedPane.addTab(DURATION_MINS, null, timePanel, null);
		timePanel.setLayout(new BorderLayout(0, 0));

		newTimeField();
		timePanel.add(timeTextField, BorderLayout.NORTH);
		timeTextField.setColumns(10);

		JPanel timeButtonPanel = new JPanel();
		timePanel.add(timeButtonPanel, BorderLayout.CENTER);
		timeButtonPanel.setLayout(new GridLayout(4, 3, 0, 0));

		JButton durationSevenButton = new JButton("7");
		durationSevenButton.addActionListener(
				(ActionEvent e) -> timeTextField.setText(timeTextField.getText()+"7")
		);
		timeButtonPanel.add(durationSevenButton);

		JButton durationEightButton = new JButton("8");
		timeButtonPanel.add(durationEightButton);
		durationEightButton.addActionListener(
				(ActionEvent e) -> timeTextField.setText(timeTextField.getText()+"8")
		);

		JButton durationNineButton = new JButton("9");
		timeButtonPanel.add(durationNineButton);
		durationNineButton.addActionListener(
				(ActionEvent e) -> timeTextField.setText(timeTextField.getText()+"9")
		);


		JButton durationFourButton = new JButton("4");
		timeButtonPanel.add(durationFourButton);
		durationFourButton.addActionListener(
				(ActionEvent e) -> timeTextField.setText(timeTextField.getText()+"4")
		);

		JButton durationFiveButton = new JButton("5");
		timeButtonPanel.add(durationFiveButton);
		durationFiveButton.addActionListener(
				(ActionEvent e) -> timeTextField.setText(timeTextField.getText()+"5")
		);
		JButton durationSixButton = new JButton("6");
		timeButtonPanel.add(durationSixButton);
		durationSixButton.addActionListener(
				(ActionEvent e) -> timeTextField.setText(timeTextField.getText()+"6")
		);

		JButton durationOneButton = new JButton("1");
		timeButtonPanel.add(durationOneButton);
		durationOneButton.addActionListener(
				(ActionEvent e) -> timeTextField.setText(timeTextField.getText()+"1")
		);


		JButton durationTwoButton = new JButton("2");
		timeButtonPanel.add(durationTwoButton);
		durationTwoButton.addActionListener(
				(ActionEvent e) -> timeTextField.setText(timeTextField.getText()+"3")
		);


		JButton durationThreeButton = new JButton("3");
		timeButtonPanel.add(durationThreeButton);
		durationThreeButton.addActionListener(
				(ActionEvent e) -> timeTextField.setText(timeTextField.getText()+"3")
		);


		JButton durationBackspaceButton = new JButton("Bksp");
		timeButtonPanel.add(durationBackspaceButton);
		durationBackspaceButton.addActionListener(
				(ActionEvent e) -> timeTextField.setText(StringUtils.chop(timeTextField.getText()))
		);


		JButton durationZeroButton = new JButton("0");
		timeButtonPanel.add(durationZeroButton);
		durationZeroButton.addActionListener(
				(ActionEvent e) -> timeTextField.setText(timeTextField.getText()+"0")
		);

		JButton durationStartButton = new JButton("Start/Stop");
		durationStartButton.addActionListener((ActionEvent e) -> {
			logger.info("Pressed Start Button");
			if (enb.isOn()) {
				logger.info("Centrifuge is running - stopping");
				enb.off();
				return;
			}
			float dutycycle = DiyCentrifuge.dutyCycleFromRpm(Double.parseDouble(DiyCentrifuge.rpmTextField.getText()));
			logger.info("Calculated Duty Cycle is " + dutycycle);
			if (stopTimer != null) {
				logger.info("Timer for stopping spin exists, cancellinging any scheduled stops");
				stopTimer.cancel();
			}
			if (!unlimitedDurationCheckbox.isSelected()) {
				logger.info("indefinite duration not selected");
				DiyCentrifuge.newTimer();
				long mins = Long.parseLong(DiyCentrifuge.timeTextField.getText());
				long msecs = mins * 60000L;
				logger.info("Stop after " + msecs + " milliseconds");
				if (stopTimer != null ) {
					stopTimer.cancel();
					newStopTimer();
					newStopTask();
					stopTimer.schedule(stopTask, msecs);
					logger.info("scheduled stop task");
				} else {
					logger.severe("Timer object for stopping spin does not exist!");
					String msg = messagesPane.getText();
					msg += "Timer Not Yet Defined!  Report This Bug\n";
					int ind = tabbedPane.indexOfTab(MESSAGES);
					tabbedPane.setSelectedIndex(ind);
					return;
				}
			}
			if (dutycycle < 25) {// overcome motor friction to get motor started when below 25% duty cycle
				enb.on(200);
				try {
					Thread.sleep(200);
				} catch (InterruptedException e1) {
					logger.log(Level.SEVERE, "Interrupted while starting motor");
					Thread.currentThread().interrupt();
				}
				enb.off();
			}

			enb.on(dutycycle);
		});
		timeButtonPanel.add(durationStartButton);

		timePanel.add(unlimitedDurationCheckbox, BorderLayout.SOUTH);

		tabbedPane.addTab(MESSAGES, null, messagesPane, null);
		JPanel infoPane = new JPanel();
		infoPane.setLayout(new BorderLayout());
		JTextArea infoText = new JTextArea();
		infoPane.add(infoText, BorderLayout.CENTER);
		infoText.setEditable(false);
		infoText.setFont(new Font(CONSOLAS, Font.PLAIN, 16));
		String info = "";
		info=info+"Enable Pin: "+DiyCentrifuge.DEFAULTENBPIN+"\n";
		info=info+"First Direction Pin: "+DiyCentrifuge.DEFAULTIN3PIN+"\n";
		info=info+"Second Direction Pin: "+DiyCentrifuge.DEFAULTIN4PIN+"\n";
		info=info+"Lowest usable RPM: "+DiyCentrifuge.DEFAULTMINRPM+"\n";
		info=info+"Highest RPM: "+DiyCentrifuge.DEFAULTMAXRPM+"\n";
		info=info+"Rotor Radius: "+DiyCentrifuge.DEFAULTRADIUS+"mm\n";
		info=info+"Lowest RCF: 95\n";
		info=info+"Max RCF: 6451\n";
		infoText.setText(info);
		tabbedPane.addTab("info", infoPane);
	}

	private void setupLogFile() {
		try {
			FileHandler fh = new FileHandler("./ui.log");
			logger.addHandler(fh);
			SimpleFormatter sf = new SimpleFormatter();
			fh.setFormatter(sf);
		} catch (SecurityException | IOException e1) {
			logger.log(Level.SEVERE, "Error setting log file", e1);
		}
	}

	private static void newFrame() {
		DiyCentrifuge.frame = new JFrame();
	}

	private static void newStopTask() {
		stopTask = new TimerTask() {
			public void run() {
				logger.info("Scheduled Stop Task Executing");
				DiyCentrifuge.enb.off();
			}
		};
	}

	private static void newStopTimer() {
		stopTimer = new Timer("Stop");
	}

	private static void pinSetup() {
		I2CProvider i2CProvider = ctx.provider("linuxfs-i2c");
		I2CConfig i2cConfig = I2C.newConfigBuilder(ctx).id("TCA9534").bus(1).device(0x3f).build();
		in3 = ctx.dout().create(16);
		in4 = ctx.dout().create(20);
		PwmConfig enconf = Pwm.newConfigBuilder(ctx).address(19).id("en").name("en").pwmType(PwmType.SOFTWARE)
				.provider("pigpio-pwm").initial(0).shutdown(0).build();
		enb = ctx.create(enconf);
	}

	private void motorSpinCheck() {
		in3.low();
		in4.low();
		in4.high();
		enb.setDutyCycle(DiyCentrifuge.MIN_STARTUP_DUTYCYCLE);
		enb.on();
		try {
			Thread.sleep(200);
			enb.off();
			enb.setDutyCycle(12);
			enb.on();
			Thread.sleep(3000);
			enb.off();
		} catch (InterruptedException e1) {
			logger.log(Level.SEVERE, "Error testing spinup", e1);
			Thread.currentThread().interrupt();
		}
	}

	private static void newTimeField() {
		timeTextField = new JTextField();
	}

	private static void newRpmTextField() {
		rpmTextField = new JTextField();
	}

}
