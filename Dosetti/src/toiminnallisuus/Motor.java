package toiminnallisuus;

import com.pi4j.component.motor.impl.GpioStepperMotorComponent;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class Motor extends Part {
	private String name; //our motor for pill dispenser is a step motor
	private boolean status; //on (=true) or off (=false)
	private double basicStep; //normal step that motor need to take to travel one slot width
	private int position; //=how many steps taken
	//private int pin; selvitä tätä varten moottorin pinni vai pinnilista Part- luokasta?
	
	
	public Motor() {
		this.name = "askelmoottori"; //vai alustetaanko? = new GpioStepperMotorComponent(pins);
		this.status = false;
		this.basicStep = 145.56; //kts. metodista step() selitys 
		this.position = 0; //starting position, koko kierros on 2038 askelta
		//this.pin = null;
	}
	
	//moottorille käsky ottaa yksi basicStep vai onko valmiina step?
	/*apuna tutorial: http://www.savagehomeautomation.com/jj-stepper
	 *  // There are 32 steps per revolution on my sample motor,
        // and inside is a ~1/64 reduction gear set.
        // Gear reduction is actually: (32/9)/(22/11)x(26/9)x(31/10)=63.683950617
        // This means is that there are really 32*63.683950617 steps per revolution
        // =  2037.88641975 ~ 2038 steps! 
         * --> joten yksi askel on 2037.88641975/14 = 145.5633156... askelta per lokero?*/
	
	public void step() {
		if (this.getStatus() == false) {
			//käynnistä moottori
			//this.switchButton();
			//this.pin =  PinState.HIGH; //tms?
			this.status = true;
			
			if (this.position + this.basicStep < 2037.8864) {
				this.position += this.basicStep;
			} else {
				//nollaa?
			}
			
		}	
	}
	
	//moottorin nollaaminen alkukohtaan
	public void resetStartPosition() {
		//motor REVERSE otetut askeleet? jolloin ->
		this.position = 0;
	}
	
	
	@Override
	public String toString() {
		if (this.status) {
			return "The motor " + this.name + " is ON.";
		} else {
			return "The motor " + this.name + " is OFF.";
		}
	}
	
	public void TakeStep(int amount) throws InterruptedException 
	{
		int fullRotation = 2038;
		long slot = fullRotation / Main.pl.slots.length;
		
		long move = slot * amount;
		
        // create gpio controller
        final GpioController gpio = GpioFactory.getInstance();

        // provision gpio pins #00 to #03 as output pins and ensure in LOW state
        final GpioPinDigitalOutput[] pins = {
                gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, PinState.LOW),
                gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, PinState.LOW),
                gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, PinState.LOW),
                gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, PinState.LOW)};

        // this will ensure that the motor is stopped when the program terminates
        gpio.setShutdownOptions(true, PinState.LOW, pins);

        // create motor component
        GpioStepperMotorComponent motor = new GpioStepperMotorComponent(pins);

        // @see http://www.lirtex.com/robotics/stepper-motor-controller-circuit/
        //      for additional details on stepping techniques

        // create byte array to demonstrate a single-step sequencing
        // (This is the most basic method, turning on a single electromagnet every time.
        //  This sequence requires the least amount of energy and generates the smoothest movement.)
        byte[] single_step_sequence = new byte[4];
        single_step_sequence[0] = (byte) 0b0001;
        single_step_sequence[1] = (byte) 0b0010;
        single_step_sequence[2] = (byte) 0b0100;
        single_step_sequence[3] = (byte) 0b1000;

        // define stepper parameters before attempting to control motor
        // anything lower than 2 ms does not work for my sample motor using single step sequence
        motor.setStepInterval(2);
        motor.setStepSequence(single_step_sequence);

        // There are 32 steps per revolution on my sample motor, and inside is a ~1/64 reduction gear set.
        // Gear reduction is actually: (32/9)/(22/11)x(26/9)x(31/10)=63.683950617
        // This means is that there are really 32*63.683950617 steps per revolution =  2037.88641975 ~ 2038 steps!
        
        //Melissa: So, if our pill dispenser has 14 slots in it, one slot equals 2038 / 14 = 145,5714285... ~ 146 steps 
        motor.setStepsPerRevolution(fullRotation);

        motor.step(move);
        
        motor.stop();

        // stop all GPIO activity/threads by shutting down the GPIO controller
        // (this method will forcefully shutdown all GPIO monitoring threads and scheduled tasks)
        gpio.shutdown();
	}

	


}
