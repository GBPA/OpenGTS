// ----------------------------------------------------------------------------
// Copyright 2007-2017, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Change History:
//  2016/12/21  Martin D. Flynn
//     -Initial release [EXPERIMENTAL - might not be used]
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.*;

import org.opengts.util.*;

public class JobAssignment
{

    // ------------------------------------------------------------------------

    public enum JobStatus implements EnumTools.StringLocale, EnumTools.IntValue {
        UNDEFINED   (   0, I18N.getString(JobAssignment.class,"JobAssignment.JobStatus.undefined"  ,"undefined"  )),
        UNAVAILABLE ( 100, I18N.getString(JobAssignment.class,"JobAssignment.JobStatus.unavailable","Unavailable")),
        AVAILABLE   ( 200, I18N.getString(JobAssignment.class,"JobAssignment.JobStatus.available"  ,"Available"  )),
        ASSIGNED    ( 300, I18N.getString(JobAssignment.class,"JobAssignment.JobStatus.assigned"   ,"Assigned"   )),
        WAITING     ( 400, I18N.getString(JobAssignment.class,"JobAssignment.JobStatus.waiting"    ,"Waiting"    )),
        IN_PROCCESS ( 500, I18N.getString(JobAssignment.class,"JobAssignment.JobStatus.inProcess"  ,"InProcess"  )),
        COMPLETED   ( 600, I18N.getString(JobAssignment.class,"JobAssignment.JobStatus.completed"  ,"Completed"  ));
        // ---
        private int         vv = 0;
        private I18N.Text   aa = null;
        JobStatus(int v, I18N.Text a)           { vv = v; aa = a; }
        public int     getIntValue()            { return vv; }
        public String  toString()               { return aa.toString(); }
        public String  toString(Locale loc)     { return aa.toString(loc); }
    };

    /**
    *** Returns the JobStatus enumeration for the specified status name
    *** @param stat  The JobStatus name
    *** @return The JobStatus enumeration
    **/
    public static JobStatus GetJobStatusForName(String stat, JobStatus dft)
    {
        return EnumTools.getValueOf(JobStatus.class,stat,dft);
    }

    // ------------------------------------------------------------------------
    
    public static final String PROP_JobID           = "JobID";
    public static final String PROP_JobName         = "JobName";
    public static final String PROP_JobType         = "JobType";
    public static final String PROP_JobStatus       = "JobStatus";
    public static final String PROP_DestName        = "DestName";
    public static final String PROP_DestLoc         = "DestLoc";
    public static final String PROP_EstTime         = "EstTime";
    public static final String PROP_ActTime         = "ActTime";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String      jobID                       = "";
    private String      jobName                     = "";
    private String      jobType                     = "";
    private JobStatus   jobStatus                   = JobStatus.UNDEFINED;
    
    private String      destName                    = null;
    private GeoPoint    destLocation                = GeoPoint.INVALID_GEOPOINT;

    private long        estCompletionTime           = 0L;
    private long        actCompletionTime           = 0L;

    public JobAssignment()
    {
        super();
    }

    public JobAssignment(String flds)
    {
        this();
        RTProperties rtp = new RTProperties(flds);
        this.setJobID(                           rtp.getString(PROP_JobID    ,""));
        this.setJobName(                         rtp.getString(PROP_JobName  ,""));
        this.setJobType(                         rtp.getString(PROP_JobType  ,""));
        this.setJobStatus(                       rtp.getString(PROP_JobStatus,""));
        this.setDestinationName(                 rtp.getString(PROP_DestName ,""));
        this.setDestinationLocation(new GeoPoint(rtp.getString(PROP_DestLoc  ,"")));
        this.setEstimatedCompletionTime(         rtp.getLong(  PROP_EstTime  ,0L));
        this.setActualCompletionTime(            rtp.getLong(  PROP_ActTime  ,0L));
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Job ID
    *** @param id  The Job ID
    **/
    public void setJobID(String id)
    {
        this.jobID = StringTools.trim(id);
    }

    /**
    *** Gets the Job ID
    *** @return The Job ID
    **/
    public String getJobID()
    {
        return this.jobID;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Job Name
    *** @param name  The Job Name
    **/
    public void setJobName(String name)
    {
        this.jobName = StringTools.trim(name);
    }

    /**
    *** Gets the Job Name
    *** @return The Job Name
    **/
    public String getJobName()
    {
        return this.jobName;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Job Type
    *** @param name  The Job Type
    **/
    public void setJobType(String type)
    {
        this.jobType = StringTools.trim(type);
    }

    /**
    *** Gets the Job Type
    *** @return The Job Type
    **/
    public String getJobType()
    {
        return this.jobType;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Job Status
    *** @param stat  The Job Status
    **/
    public void setJobStatus(String stat)
    {
        this.setJobStatus(GetJobStatusForName(stat,null));
    }

    /**
    *** Sets the Job Status
    *** @param stat  The Job Status
    **/
    public void setJobStatus(JobStatus stat)
    {
        this.jobStatus = (stat != null)? stat : JobStatus.UNDEFINED;
    }

    /**
    *** Gets the Job Status
    *** @return The Job Status (does not return null)
    **/
    public JobStatus getJobStatus()
    {
        return (this.jobStatus != null)? this.jobStatus : JobStatus.UNDEFINED;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Destination Name
    *** @param name  The Destination Name
    **/
    public void setDestinationName(String name)
    {
        this.destName = StringTools.trim(name);
    }

    /**
    *** Gets the Destination Name
    *** @return The Destination Name
    **/
    public String getDestinationName()
    {
        return this.destName;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Destination Location
    *** @param loc  The Destination Location
    **/
    public void setDestinationLocation(GeoPoint loc)
    {
        this.destLocation = GeoPoint.isValid(loc)? loc : GeoPoint.INVALID_GEOPOINT;
    }

    /**
    *** Gets the Destination Location
    *** @return The Destination Location (does not return null)
    **/
    public GeoPoint getDestinationLocation()
    {
        return GeoPoint.isValid(this.destLocation)? this.destLocation : GeoPoint.INVALID_GEOPOINT;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Estimated Completion Time
    *** @param time  The Estimated Completion Time
    **/
    public void setEstimatedCompletionTime(long time)
    {
        this.estCompletionTime = time;
    }

    /**
    *** Gets the Estimated Completion Time
    *** @return The Estimated Completion Time
    **/
    public long getEstimatedCompletionTime()
    {
        return this.estCompletionTime;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Actual Completion Time
    *** @param time  The Actual Completion Time
    **/
    public void setActualCompletionTime(long time)
    {
        this.actCompletionTime = time;
    }

    /**
    *** Gets the Actual Completion Time
    *** @return The Actual Completion Time
    **/
    public long getActualCompletionTime()
    {
        return this.actCompletionTime;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets a new RTProperties instance containing the values of this instance
    **/
    public RTProperties getRTProperties()
    {
        RTProperties rtp = new RTProperties();
        rtp.setString(PROP_JobID     , this.getJobID());
        rtp.setString(PROP_JobName   , this.getJobName());
        rtp.setString(PROP_JobType   , this.getJobType());
        rtp.setString(PROP_JobStatus , this.getJobStatus().toString());
        rtp.setString(PROP_DestName  , this.getDestinationName());
        rtp.setString(PROP_DestLoc   , this.getDestinationLocation().toString());
        rtp.setLong(  PROP_EstTime   , this.getEstimatedCompletionTime());
        rtp.setLong(  PROP_ActTime   , this.getActualCompletionTime());
        return rtp;
    }

    /**
    *** Gets a String representation of this instance
    **/
    public String toString()
    {
        return this.getRTProperties().toString();
    }

    // ------------------------------------------------------------------------

}
