package icecube.daq.trigger.algorithm;
import icecube.daq.payload.IHitPayload;
import icecube.daq.payload.IPayload;
import icecube.daq.payload.IUTCTime;
import icecube.daq.trigger.exceptions.*;
import icecube.daq.util.DOMInfo;
import icecube.daq.util.IDOMRegistry;
import org.apache.log4j.Logger;
import java.util.*;

public class FaintParticleTrigger
        extends AbstractTrigger
{
    /** Log object for this class */
    private static final Logger LOG = Logger.getLogger(FaintParticleTrigger.class);

    /** I3Live monitoring name for this algorithm */
    private static final String MONITORING_NAME = "FAINT_PARTICLE";

    /** Numeric type for this algorithm */
    public static final int TRIGGER_TYPE = 33;

    //Trigger parameters
    public long time_window;
    public long time_window_separation;
    public  double max_trigger_length;


    //first stage
    public int hit_min;
    public int hit_max;
    //second stage
    public double double_velocity_min;
    public double double_velocity_max;
    public int double_min;

    public boolean use_dc_version;
    //stage 3 version 1
    public int triple_min;

    //stage 3 version 2
    public int histogram_binning;
    public int azimuth_histogram_min;
    public int zenith_histogram_min;


    //stage 4
    public double slcfraction_min;


    public boolean time_window_configured = false;
    public boolean time_window_separation_configured = false;
    public boolean max_trigger_length_configured= false;

    public boolean hit_min_configured = false;
    public boolean hit_max_configured = false;

    public boolean double_velocity_min_configured = false;
    public boolean double_velocity_max_configured = false;
    public boolean double_min_configured = false;

    public boolean use_dc_version_configured = false;
    public boolean triple_min_configured = false;

    public boolean histogram_binning_configured = false;
    public boolean azimuth_histogram_min_configured = false;
    public boolean zenith_histogram_min_configured = false;

    public boolean slcfraction_min_configured = false;

    public IUTCTime StartTime = null;
    // Convert to ns
    final int convert_to_ns = 10;
    //Convert to km/s
    final double convert_to_km_s = 1e7;
    //Keep track of the trigger window end
    public long trigger_window_end;





    /**
     * list of hits currently within slidingTimeWindow
     */
    private SlidingTimeWindow slidingTimeWindow = new SlidingTimeWindow();

    /**
     * list of hits in current trigger
     */
    private HitCollection hitsWithinTriggerWindow = new HitCollection();

    public IUTCTime lastHitTime = null;



    public FaintParticleTrigger()
    {


    }
    @Override
    public void addParameter(String name, String value)
            throws UnknownParameterException, IllegalParameterValueException
    {
        if (name.compareTo("time_window") == 0) {
            time_window = Long.parseLong(value);
            time_window_configured= true;
        } else if (name.compareTo("time_window_separation") == 0) {
            time_window_separation = Long.parseLong(value);
            time_window_separation_configured= true;
        } else if (name.compareTo("max_trigger_length") == 0) {
            max_trigger_length =  Double.parseDouble(value);
            max_trigger_length_configured= true;
        } else if (name.compareTo("hit_min") == 0) {
            hit_min= Integer.parseInt(value);
            hit_min_configured= true;
        } else if (name.compareTo("hit_max") == 0) {
            hit_max= Integer.parseInt(value);
            hit_max_configured= true;
        } else if (name.compareTo("double_velocity_min") == 0) {
            double_velocity_min= Double.parseDouble(value);
            double_velocity_min_configured= true;
        } else if (name.compareTo("double_velocity_max") == 0) {
            double_velocity_max =  Double.parseDouble(value);
            double_velocity_max_configured= true;
        } else if (name.compareTo("double_min") == 0) {
            double_min = Integer.parseInt(value);
            double_min_configured= true;
        }  else if (name.compareTo("use_dc_version") == 0) {
            use_dc_version=  Boolean.parseBoolean(value);
            use_dc_version_configured= true;
        } else if (name.compareTo("triple_min") == 0) {
            triple_min= Integer.parseInt(value);
            triple_min_configured= true;
        } else if (name.compareTo("histogram_binning") == 0) {
            histogram_binning= Integer.parseInt(value);
            histogram_binning_configured= true;
        } else if (name.compareTo("azimuth_histogram_min") == 0) {
            azimuth_histogram_min= Integer.parseInt(value);
            azimuth_histogram_min_configured= true;
        } else if (name.compareTo("zenith_histogram_min") == 0) {
            zenith_histogram_min= Integer.parseInt(value);
            zenith_histogram_min_configured= true;
        } else if (name.compareTo("slcfraction_min") == 0) {
            slcfraction_min= Double.parseDouble(value);
            slcfraction_min_configured= true;
        } else if (name.compareTo("domSet") == 0) {
            domSetId = Integer.parseInt(value);
            try {
                configHitFilter(domSetId);
            } catch (ConfigException ce) {
                throw new IllegalParameterValueException("Bad DomSet #" +
                        domSetId, ce);
            }
        } else {
            throw new UnknownParameterException("Unknown parameter: " +
                    name);
        }
        super.addParameter(name, value);
    }

    /**
     * Get the trigger type.
     *
     * @return trigger type
     */
    @Override
    public int getTriggerType()
    {
        return TRIGGER_TYPE;
    }

    /**
     * Does this algorithm include all relevant hits in each request
     * so that it can be used to calculate multiplicity?
     *
     * @return <tt>true</tt> if this algorithm can supply a valid multiplicity
     */
    @Override
    public boolean hasValidMultiplicity()
    {
        return false;
    }


    @Override
    public void flush()
    {
        if (haveTrigger()) {
	    long overlap = trigger_window_end -StartTime.longValue();
	    if (overlap<0){
            	flushTrigger();
	   }
        }
    }


    /**
     * Set name of trigger
     * @param triggerName
     */
    @Override
    public void setTriggerName(String triggerName)
    {
        super.triggerName = triggerName;
        if (LOG.isInfoEnabled()) {
            LOG.info("TriggerName set to " + super.triggerName);
        }
    }

    @Override
    public String getMonitoringName()
    {
        return MONITORING_NAME;
    }

    @Override
    public boolean isConfigured()
    {
        if (use_dc_version_configured)
        {
            if (use_dc_version)
            {
                return ( time_window_configured && time_window_separation_configured && max_trigger_length_configured &&
                        hit_min_configured && hit_max_configured && double_velocity_min_configured && double_velocity_max_configured &&double_min_configured
                        && histogram_binning_configured && azimuth_histogram_min_configured && zenith_histogram_min_configured
                        && slcfraction_min_configured );
            }
            else
            {
                return ( time_window_configured && time_window_separation_configured && max_trigger_length_configured &&
                        hit_min_configured && hit_max_configured && double_velocity_min_configured && double_velocity_max_configured &&double_min_configured
                        &&  triple_min_configured && slcfraction_min_configured  );
            }
        }
        return false;
    }

    public void runTrigger(IPayload payload) throws TriggerException {



        if (!(payload instanceof IHitPayload))
            throw new TriggerException(
                    "Payload object " + payload + " cannot be upcast to IHitPayload."
            );
        // This upcast should be safe now
        IHitPayload hit = (IHitPayload) payload;
        IUTCTime hitTimeUTC = hit.getPayloadTimeUTC();
        if (hitTimeUTC == null) {
            throw new TriggerException("Hit time was null");
        }

        // verify strict time ordering
        if (lastHitTime != null && hitTimeUTC.compareTo(lastHitTime) < 0) {
            throw new TimeOutOfOrderException(
                    "Hit comes before previous hit:" +
                            " Previous hit is at " + lastHitTime +
                            " Hit is at " + hitTimeUTC + " DOMId = " +
                            hit.getDOMID());
        }
        lastHitTime = hitTimeUTC;

        boolean usableHit = hitFilter.useHit(hit) && getHitType(hit)==SPE_HIT;

        if (!usableHit) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Hit " + hit + " isn't usable");
            }
            return;
        }
        analyzeWindow(hit);
    }



    public void analyzeWindow(IHitPayload hit) {

        if (StartTime == null) {
            StartTime = hit.getPayloadTimeUTC();

        }
        // The time window will be analyzed if the current hit is outside of the window
        updateSlidingWindow(hit.getPayloadTimeUTC());

        // The current hit is added to the window and will be analyzed in the next iteration
        if (!slidingTimeWindow.contains(hit)) {
            slidingTimeWindow.add(hit);

        }
    }

    public void FPTalgorithm(){
        IDOMRegistry domRegistry = getTriggerManager().getDOMRegistry();
	flush();
        //First cut on the number of hits
        if (slidingTimeWindow.HitThreshold()) {
            // Second cut on the number of Doubles (All hit pair combinations that satisfy a velocity cut)
            ArrayList<Integer> Double_Indices = slidingTimeWindow.DoubleThreshold(domRegistry);
            int number_doubles = Double_Indices.size() / 2;
            if (number_doubles >= double_min ) {
                // direction or triple cut
                boolean cut3 = false;
                if ( use_dc_version){
                    //Third cut on the clustering of Doubles in zenith and azimuth
                    ArrayList<Integer> direction = slidingTimeWindow.DirectionThreshold(Double_Indices,domRegistry);
                    int number_azimuth = direction.get(0);
                    int number_zenith = direction.get(1);
                    if (number_zenith> zenith_histogram_min && number_azimuth > azimuth_histogram_min){
                        cut3 = true;
                    }


                }
                else {
                    //Third cut on the number of Triples (combinations of three hits that satisfy a velocity cut)

                    int number_triples = slidingTimeWindow.TripleThreshold(Double_Indices,domRegistry);
                    if (number_triples > triple_min){
                        cut3 = true;
                    }

                }
                if (cut3){
                    //Fourth cut on the SLC fraction

                    double slc_fraction = slidingTimeWindow.SlcFractionThreshold();
                    if (slc_fraction > slcfraction_min) {

                        //Check if previous window is above threshold and add new hits to the trigger window
                        if (haveTrigger()) {
							trigger_window_end =StartTime.getOffsetUTCTime(time_window*convert_to_ns).longValue();
                            for (IHitPayload k: slidingTimeWindow ){
                                if (!hitsWithinTriggerWindow.contains(k)){
                                    hitsWithinTriggerWindow.add(k);

                                }
                            }

                            double trigger_length =hitsWithinTriggerWindow.getLast().getUTCTime() - hitsWithinTriggerWindow.getFirst().getUTCTime();
                            if ( trigger_length> max_trigger_length*convert_to_ns) {
			        LOG.error("Unexpected long event");
                                flushTrigger();
                            }
                                /*
                                 If there was no previous trigger the hits are added to the trigger window which could be extended
                                in the next iteration up to the maximum trigger length

                                 */
                        } else {
							trigger_window_end =StartTime.getOffsetUTCTime(time_window*convert_to_ns).longValue();
                            hitsWithinTriggerWindow.addAll(slidingTimeWindow.copy());
                       }
                    }
                }
            }
        }
    }




    public void updateSlidingWindow(IUTCTime hitTimeUTC)
    {

        /*
        If the hit lies outside of the timewindow, the current time window will be analyzed and bounds
        adjusted unitl the new hit is within the new time window bounds. Afterwards it is added to the time window, which will be analyzed when
        the next hit is outside of the window and triggers the while loop.

         */
        while (!slidingTimeWindow.inTimeWindow(hitTimeUTC, StartTime)) {

            //If there is no hit inside of the time window adjust the bounds until the new hit is in the window and skip the rest of the function.
            if (slidingTimeWindow.size()==0) {

                StartTime = StartTime.getOffsetUTCTime(time_window_separation*convert_to_ns);
                //this will not be a time window extending the trigger -> flush if previous one was triggered
                flush();
                continue;
            }


            // Run the trigger algorithm on the hits that are within the time window
            FPTalgorithm();
            /*
            Since all other hits are within the current window and the hit that is currently analyzed is not yet in the window
            The bound of the time window can be shifted. The windows are separated by a fixed time timeWindow_separation
             */
            StartTime = StartTime.getOffsetUTCTime(time_window_separation*convert_to_ns);
            //Since the bounds are shifted the first hit(s) of the time window can now lie before the new window and are removed

            while (slidingTimeWindow.getFirst().getPayloadTimeUTC().compareTo(StartTime)<0) {

                if (slidingTimeWindow.size()==1) {
                    slidingTimeWindow.removeFirst();
                    flush();

                    break;

                }
                slidingTimeWindow.removeFirst();



            }


        }

    }



    // Calculates the histogram for the given angle array in specified binning. Returns the maximum value of the histogram count
    private ArrayList<Integer>  CalcHistogram(ArrayList<Double> Angles,int lower_bound, int upper_bound, int bin_size) {

        ArrayList<Integer> hist_vals = new ArrayList<>();
        ArrayList<Integer> Returnval= new ArrayList<>();
        for (int j = lower_bound; j<upper_bound;j+=bin_size){
            //include 180 and 360 ° in the last bins 160-180. Otherwise no meaningful mean value for the angle can be calculated
            int upper_bin_size = j+bin_size;
            if (j==upper_bound-bin_size){upper_bin_size = j+bin_size+1;}
            int hist_counter = 0;
            ArrayList<Double> angle_values = new ArrayList<>();
            for (double k : Angles){
                if (k>=j && k< upper_bin_size){
                    hist_counter +=1;
                    angle_values.add(k);
                }
            }
            hist_vals.add(hist_counter);


            //Select the bin with maximum counts



        }
        Returnval.add (Collections.max(hist_vals));
        return Returnval;
    }


    private void flushTrigger() {
        formTrigger(hitsWithinTriggerWindow.list(), null, null);
        hitsWithinTriggerWindow.clear();
    }
    private boolean haveTrigger()
    {
        return hitsWithinTriggerWindow.size() > 0;
    }

    private void reset()
    {
        slidingTimeWindow.clear();
        hitsWithinTriggerWindow.clear();
        lastHitTime = null;
    }
    @Override
    public void resetAlgorithm()
    {
        reset();

        super.resetAlgorithm();
    }
    public Integer getSlidingTimeWindowContent(){

            return slidingTimeWindow.size();
    }
 
    final class SlidingTimeWindow
            extends HitCollection
    {

        public boolean HitThreshold() {return (size() >= hit_min && size() <= hit_max);}
        public ArrayList<Integer> DoubleThreshold(IDOMRegistry domRegistry )
        {
            ArrayList<Integer> Indices = new ArrayList<>();

            //List is time sorted: Dont compare combinations with itsself and commutative combinations

            for (int j = 0;j<size();j++){
                for(int k =j+1;k<size();k++) {
                    DOMInfo hit1_dom = getDOMFromHit(domRegistry, get(j));
                    DOMInfo hit2_dom = getDOMFromHit(domRegistry, get(k));
                    if (hit1_dom != hit2_dom){
                        long hit1_time = get(j).getUTCTime();
                        long hit2_time = get(k).getUTCTime();
                        double dist_jk = domRegistry.distanceBetweenDOMs(hit1_dom, hit2_dom);
                        //timediff should be positive anyway due to time ordering of the list
                        double timediff_jk = Math.abs(hit2_time-hit1_time);
                        //velocity in km/s
                        double vel_jk = dist_jk/timediff_jk*convert_to_km_s;

                        if (vel_jk>double_velocity_min && vel_jk < double_velocity_max){
                            Collections.addAll(Indices,j,k);
                            //Threshold exceeded: return empty list -> below threshold

                        }
                    }

                }

            }
            return Indices;
        }

        private int TripleThreshold(ArrayList<Integer> Doub_Indices, IDOMRegistry domRegistry) {
            int triple_combinations = 0;

            for (int j = 0; j < Doub_Indices.size()-2; j+= 2) {
                for (int k = j + 2; k <= Doub_Indices.size()-2; k += 2) {
                    //Check for two doubles that share the middle hit in time (0,1) (1,2) -> (0,1,2)
                    if (Doub_Indices.get(j+1)== Doub_Indices.get(k)) {
                        // As the doubles are velocity consistent only the third component (0-2) is checked
                        DOMInfo hit1_dom = getDOMFromHit(domRegistry, get(Doub_Indices.get(j)));
                        DOMInfo hit2_dom = getDOMFromHit(domRegistry, get(Doub_Indices.get(k+1)));
                        long hit1_time =get(Doub_Indices.get(j)).getUTCTime();
                        long hit2_time = get(Doub_Indices.get(k+1)).getUTCTime();
                        if (hit1_dom != hit2_dom){
                            double dist_jk = domRegistry.distanceBetweenDOMs(hit1_dom,hit2_dom);
                            //timediff should be positive anyway due to time ordering of the list
                            double timediff_jk = Math.abs(hit2_time-hit1_time);
                            //velocity in km/s
                            double vel_jk = dist_jk/timediff_jk*convert_to_km_s;
                            if (vel_jk>double_velocity_min && vel_jk < double_velocity_max){
                                triple_combinations+=1;

                            }
                        }
                    }

                }

            }


            return triple_combinations;

        }

        public ArrayList<Integer>  DirectionThreshold(ArrayList<Integer> Doub_Indices,IDOMRegistry domRegistry) {
            ArrayList<Integer> final_zen_azi = new ArrayList<>();
            ArrayList<Double> Zenith_values = new ArrayList<>();
            ArrayList<Double> Azimuth_values = new ArrayList<>();

            for (int j = 0; j < Doub_Indices.size(); j+= 2) {
                DOMInfo hit1_dom = getDOMFromHit(domRegistry, get(Doub_Indices.get(j)));
                DOMInfo hit2_dom = getDOMFromHit(domRegistry, get(Doub_Indices.get(j+1)));
                double zenith = domRegistry.directionBetweenDOMs(hit1_dom, hit2_dom)[0];
                double azimuth = domRegistry.directionBetweenDOMs(hit1_dom, hit2_dom)[1];
                Zenith_values.add(Math.toDegrees(zenith));
                Azimuth_values.add(Math.toDegrees(azimuth));

            }
            ArrayList<Integer> hist_zenith= CalcHistogram(Zenith_values,0,180,histogram_binning);
            ArrayList<Integer> hist_azimuth= CalcHistogram(Azimuth_values,0,360,histogram_binning);
            final_zen_azi.addAll(hist_azimuth);
            final_zen_azi.addAll(hist_zenith);

            return final_zen_azi;
        }

        public double SlcFractionThreshold() {

            int hlc_count = 0;
            int slc_count = 0;
            for (IHitPayload hit2 : this) {
                if (hit2.isSLC()) {
                    slc_count += 1;
                } else {
                    hlc_count += 1;
                }
            }
            double slc_fraction = (double) slc_count /(slc_count + hlc_count);
            return slc_fraction;
        }
        private IUTCTime endTime()
        {
            return (startTime().getOffsetUTCTime(time_window*convert_to_ns));
        }



        private boolean inTimeWindow(IUTCTime hitTime, IUTCTime StartT)
        {

            return hitTime.compareTo(StartT) >= 0 &&
                    hitTime.compareTo(StartT.getOffsetUTCTime(time_window*convert_to_ns)) < 0;

        }

        private IHitPayload slide()
        {
            return removeFirst();
        }

        private IUTCTime startTime()
        {
            return getFirst().getPayloadTimeUTC();
        }

        public String toString()
        {
            if (size() == 0) {
                return "Window[]";
            }

            return "Window*" + size() + "[" + startTime() + "-" + endTime() +
                    "]";
        }
    }


}