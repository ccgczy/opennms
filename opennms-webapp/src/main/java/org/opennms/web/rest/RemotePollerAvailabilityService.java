package org.opennms.web.rest;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.opennms.netmgt.dao.ApplicationDao;
import org.opennms.netmgt.dao.LocationMonitorDao;
import org.opennms.netmgt.dao.MonitoredServiceDao;
import org.opennms.netmgt.model.OnmsApplication;
import org.opennms.netmgt.model.OnmsLocationMonitor;
import org.opennms.netmgt.model.OnmsLocationSpecificStatus;
import org.opennms.netmgt.model.OnmsMonitoredService;
import org.opennms.netmgt.model.OnmsMonitoringLocationDefinition;
import org.opennms.netmgt.model.PollStatus;
import org.opennms.web.svclayer.support.DefaultDistributedStatusService.Severity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sun.jersey.spi.resource.PerRequest;

@Component
@PerRequest
@Path("remotepoller")
@Transactional
public class RemotePollerAvailabilityService extends OnmsRestService {


    @Autowired
    LocationMonitorDao m_locationMonitorDao;
    
    @Autowired
    ApplicationDao m_applicationDao;
    
    @Autowired
    MonitoredServiceDao m_monitoredServiceDao;
    
    @Context
    UriInfo m_uriInfo;
    
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public String getAvailability() {
        List<String> percentageList = new ArrayList<String>();
        
        List<OnmsLocationMonitor> monitors = m_locationMonitorDao.findAll();
        Collection<OnmsApplication> applications = m_applicationDao.findAll();
        List<OnmsMonitoringLocationDefinition> locationDefinitions = m_locationMonitorDao.findAllMonitoringLocationDefinitions();
        
        if (applications.size() == 0) {
            throw new IllegalArgumentException("there are no applications");
        }
        
        List<OnmsApplication> sortedApplications = new ArrayList<OnmsApplication>(applications);
        Collections.sort(sortedApplications);
        
        Collection<OnmsLocationSpecificStatus> statusesPeriod = new HashSet<OnmsLocationSpecificStatus>();
        Date start = new GregorianCalendar(Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH).getTime();
        Date end = new Date();
        statusesPeriod.addAll(m_locationMonitorDao.getAllStatusChangesAt(start));
        statusesPeriod.addAll(m_locationMonitorDao.getStatusChangesBetween(start, end));
        
        for(OnmsMonitoringLocationDefinition locationDefinition : locationDefinitions) {
            Collection<OnmsLocationMonitor> m = m_locationMonitorDao.findByLocationDefinition(locationDefinition);
            
            for(OnmsApplication application : sortedApplications) {
                Collection<OnmsMonitoredService> memberServices = m_monitoredServiceDao.findByApplication(application);
                Set<OnmsLocationSpecificStatus> selectedStatuses = filterStatus(statusesPeriod, monitors, memberServices);
                
                percentageList.add(calculatePercentageUptime(memberServices, selectedStatuses, start, end));
                
            }
        }
        
        return "percentageList Size: " + percentageList.size() + " sorted Applications: " + sortedApplications;
    }
    
    private String calculatePercentageUptime(Collection<OnmsMonitoredService> applicationServices, Set<OnmsLocationSpecificStatus> statuses, Date startDate, Date endDate) {
        /*
         * The methodology is as such:
         * 1) Sort the status entries by their timestamp;
         * 2) Create a Map of each monitored service with a default
         *    PollStatus of unknown.
         * 3) Iterate through the sorted list of status entries until
         *    we hit a timestamp that is not within our time range or
         *    run out of entries.
         *    a) Along the way, update the status Map with the current
         *       entry's status, and calculate the current status.
         *    b) If the current timestamp is before the start time, store
         *       the current status so we can use it once we cross over
         *       into our time range and then continue.
         *    c) If the previous status is normal, then count up the number
         *       of milliseconds since the previous state change entry in
         *       the time range (or the beginning of the range if this is
         *       the first entry in within the time range), and add that
         *       a counter of "normal" millseconds.
         *    d) Finally, save the current date and status for later use.
         * 4) Perform the same computation in 3c, except count the number
         *    of milliseconds since the last state change entry (or the
         *    start time if there were no entries) and the end time, and add
         *    that to the counter of "normal" milliseconds.
         * 5) Divide the "normal" milliseconds counter by the total number
         *    of milliseconds in our time range and compute and return a
         *    percentage.
         */

        List<OnmsLocationSpecificStatus> sortedStatuses =
            new LinkedList<OnmsLocationSpecificStatus>(statuses);
        Collections.sort(sortedStatuses, new Comparator<OnmsLocationSpecificStatus>(){
            public int compare(OnmsLocationSpecificStatus o1, OnmsLocationSpecificStatus o2) {
                return o1.getPollResult().getTimestamp().compareTo(o2.getPollResult().getTimestamp());
            }
        });

        HashMap<OnmsMonitoredService,PollStatus> serviceStatus =
            new HashMap<OnmsMonitoredService,PollStatus>();
        for (OnmsMonitoredService service : applicationServices) {
            serviceStatus.put(service, PollStatus.unknown("No history for this service from this location"));
        }
        
        float normalMilliseconds = 0f;
        
        Date lastDate = startDate;
        Severity lastStatus = Severity.CRITICAL;
        
        for (OnmsLocationSpecificStatus status : sortedStatuses) {
            Date currentDate = status.getPollResult().getTimestamp();

            if (!currentDate.before(endDate)) {
                // We're at or past the end date, so we're done processing
                break;
            }
            
            serviceStatus.put(status.getMonitoredService(), status.getPollResult());
            Severity currentStatus = calculateStatus(serviceStatus.values());
            
            if (currentDate.before(startDate)) {
                /*
                 * We're not yet to a date that is inside our time period, so
                 * we don't need to check the status and adjust the
                 * normalMilliseconds variable, but we do need to save the
                 * status so we have an up-to-date status when we cross the
                 * start date.
                 */
                lastStatus = currentStatus;
                continue;
            }
            
            /*
             * Because we *just* had a state change, we want to look at the
             * value of the *last* status.
             */
            if (lastStatus == Severity.NORMAL) {
                long milliseconds = currentDate.getTime() - lastDate.getTime();
                normalMilliseconds += milliseconds;
            }
            
            lastDate = currentDate;
            lastStatus = currentStatus;
        }
        
        if (lastStatus == Severity.NORMAL) {
            long milliseconds = endDate.getTime() - lastDate.getTime();
            normalMilliseconds += milliseconds;
        }

        float percentage = normalMilliseconds /
            (endDate.getTime() - startDate.getTime()) * 100;
        return new DecimalFormat("0.000").format((double) percentage) + "%";
    }

    private Severity calculateStatus(Collection<PollStatus> pollStatuses) {
        int goodStatuses = 0;
        int badStatuses = 0;
        
        for (PollStatus pollStatus : pollStatuses) {
            if (pollStatus.isAvailable()) {
                goodStatuses++;
            } else if (!pollStatus.isUnknown()) {
                badStatuses++;
            }
        }

        if (goodStatuses == 0 && badStatuses == 0) {
            return Severity.INDETERMINATE;
        } else if (goodStatuses > 0 && badStatuses == 0) {
            return Severity.NORMAL;
        } else {
            return Severity.CRITICAL;
        }
    }

    private Set<OnmsLocationSpecificStatus> filterStatus(Collection<OnmsLocationSpecificStatus> statuses, List<OnmsLocationMonitor> monitors, Collection<OnmsMonitoredService> services) {
        Set<OnmsLocationSpecificStatus> filteredStatuses = new HashSet<OnmsLocationSpecificStatus>();
        
        for (OnmsLocationSpecificStatus status : statuses) {
            if (!monitors.contains(status.getLocationMonitor())) {
                continue;
            }
        
            if (!services.contains(status.getMonitoredService())) {
                continue;
            }

            filteredStatuses.add(status);
        }

        return filteredStatuses;
    }

    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Path("{location}")
    public String getLocationAvailability(@PathParam("location") String location) {
        return "location";
    }
}
