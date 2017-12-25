#!/bin/bash
# -----------------------------------------------------------------------------
# Project: OpenGTS - Open GPS Tracking System
# URL    : http://www.opengts.org
# File   : checkInstall.sh
# -----------------------------------------------------------------------------
# Usage: : [see "function usage()" below]
# -----------------------------------------------------------------------------
if [ "${GTS_HOME}" = "" ]; then 
    echo "WARNING: GTS_HOME not defined!"
    GTS_HOME=".";  # - default to current dir
fi
# -----------------------------------------------------------------------------
GTS_DEBUG=0
GTS_VERBOSE=0
GTS_SENDMAIL=""
GTS_SMTPPL=""
GTS_SENDSMS=""
GTS_FILEMODS=0


# --- "GTS_CONF" config file name
if [ "$GTS_CONF" = "" ]; then
    GTS_CONF="${GTS_HOME}/default.conf"
fi

# --- current directory
CURRENT_DIR=`(cd -P .;pwd)`
SHOW_CURRENT_DIR=0

# --- set location of Jar libraries
JARDIR="build/lib"
if [ ! -f "${JARDIR}/tools.jar" ]; then
    echo "-----"
    echo "ERROR: '${JARDIR}/tools.jar' not found!"
    echo "Possible reasons may include one or more of the following:"
    echo " - This command is not being run from the OpenGTS installation directory"
    echo " - The OpenGTS project has not been compiled properly"
    echo "Current directory = ${CURRENT_DIR}"
    echo "-----"
    SHOW_CURRENT_DIR=1
fi

# --- 'track' classes directory
TRACKCLASSES="build/track/WEB-INF/classes"
if [ ! -d "${TRACKCLASSES}" ]; then
    echo "-----"
    echo "ERROR: '${TRACKCLASSES}' not found!"
    echo "Possible reasons may include one or more of the following:"
    echo " - This command is not being run from the OpenGTS installation directory"
    echo " - The OpenGTS project has not been compiled properly"
    echo "This condition may cause false errors/warnings to be reported!"
    echo "-----"
    SHOW_CURRENT_DIR=1
fi

# --- show current directory
if [ $SHOW_CURRENT_DIR -eq 1 ]; then
    echo "Current Directory: ${CURRENT_DIR}"
    echo "-----"
fi

# --- check Tomcat libraries
TOMCAT_LIB=
if   [ -f "${CATALINA_HOME}/common/lib/servlet-api.jar" ]; then
    TOMCAT_LIB="${CATALINA_HOME}/common/lib/"
elif [ -f "${CATALINA_HOME}/lib/servlet-api.jar" ]; then
    TOMCAT_LIB="${CATALINA_HOME}/lib/"
else
    echo "-----"
    echo "ERROR: '${CATALINA_HOME}/lib/servlet-api.jar' not found!"
    echo "Possible reasons may include one or more of the following:"
    echo " - The Tomcat 'CATALINA_HOME' environment variable was not set properly"
    echo " - Tomcat was not installed properly"
    echo "-----"
fi
TOMCAT_CATALINA="${TOMCAT_LIB}/catalina.jar"
TOMCAT_API="${TOMCAT_LIB}/servlet-api.jar"

# --- commands
CMD_JAVA="java"
CMD_LS="ls"
CMD_RM="rm"
CMD_PSJAVA="${GTS_HOME}/bin/psjava"
CMD_CHECKINSTALL="${GTS_HOME}/bin/checkInstall.sh"
CMD_EMAILFILE="${GTS_HOME}/bin/emailFile.sh"

# --- SendMail java command
CMD_EXEJAVA="${GTS_HOME}/bin/exeJava"
CMD_SENDMAIL=""
if [ -x "${CMD_EXEJAVA}" ]; then
    CMD_SENDMAIL="${CMD_EXEJAVA} -q org.opengts.util.SendMail"
fi

# --- directories
LOG_DIR="${GTS_HOME}/logs"

# --- set the classpath
if echo ${OS} | grep -q "Windows" ; then
    IS_WINDOWS=1
    PATHSEP=";"
else
    IS_WINDOWS=0
    PATHSEP=":"
fi
CPATH="${JARDIR}/tools.jar"
for jarname in \
    "gtsdb" \
    "gtsutils" \
    "dmtpserv" \
    "gtsdmtp" \
    "optdb" \
    "ruledb" \
    "ruletrack" \
    "bcrossdb" \
    "bcrosstrack" \
    "opttrack" \
    "custom" \
    "customtrack" \
    "wartools" \
    "warmaps" \
    "activation" \
    "mail" \
    "mysql-connector-java-3.1.7-bin" \
    "sqljdbc" 
do
    if [ -f "${JARDIR}/${jarname}.jar" ]; then
        CPATH="${CPATH}${PATHSEP}${JARDIR}/${jarname}.jar"
    fi
done
CPATH="${CPATH}${PATHSEP}build/track/WEB-INF/classes"
if [ "${TOMCAT_API}" != "" ]; then
    CPATH="${CPATH}${PATHSEP}${TOMCAT_API}"
fi
if [ "${TOMCAT_CATALINA}" != "" ]; then
    CPATH="${CPATH}${PATHSEP}${TOMCAT_CATALINA}"
fi

# -----------------------------------------------------------------------------

# --- usage (and exit)
function usage() {
    echo ""
    echo "Usage: bin/checkInstall.sh [<Options-1>] [-- <Options-2>]"
    echo ""
    echo " Options-1: (equal-sign must NOT be included between option and argument)"
    echo "   -full                   : CheckInstall, plus 'psjava' and listing of log file directory"
    echo "   -sendMail <addr>        : Send test email to email-address during checkinstall checks"
    echo "   -sendSMS <phone#>       : Send test SMS to phone# during checkinstall checks"
    echo "   -debug                  : Turn on verbose debug messages"
    echo "   -emailOutput <addr>     : EMail output of CheckInstall to specified email address"
    echo ""
    echo " Options-2: (equal-sign MUST be included between option and argument)"
    echo "   -localStrings=<dir>     : Validate LocalStrings_XX.properties files only"
    echo "   -showReportLimits       : Show report limits during checkinstall checks"
    echo "   -showDBErrors           : Show DB table validation errors during checkinstall checks"
    echo ""
    exit 1
}

# --- display line separator
function printSep() {
    echo "====================================================================================="
}

# -----------------------------------------------------------------------------

FULL_CHECK=0
PROPS_ONLY=0

# --- options
while (( "$#" )); do
    case "$1" in 

        # - Help
        "-help" ) 
            usage
            exit 0
            ;;

        # - Full check (psjava and log directory)
        "-full" ) 
            FULL_CHECK=1
            shift
            ;;

        # - EMail output
        "-emailOutput" ) 
            if [ -d "${LOG_DIR}" ]; then
                CILogFile=${LOG_DIR}/checkInstall.out
                ${CMD_RM} -f ${CILogFile}
                ${CMD_CHECKINSTALL} -full > ${CILogFile}
                if [ $# -ge 2 ]; then
                    EMAIL_ADDR="$2"
                    echo "Sending CheckInstall output to ${EMAIL_ADDR} ..."
                    ${CMD_EMAILFILE} -to ${EMAIL_ADDR} -file ${CILogFile}
                    exit 0
                else
                    echo "Sending CheckInstall output to default service provider ..."
                    ${CMD_EMAILFILE} -file ${CILogFile}
                    exit 0
                fi
            fi
            echo "GTS log directory not found: ${LOG_DIR}"
            exit 99
            ;;

        # - debug 
        "-debug" ) 
            GTS_DEBUG=1
            shift
            ;;

        # - rtverbose 
        "-rtverbose" ) 
            GTS_VERBOSE=1
            shift
            ;;

        # - send test email (append "-- -smtp.debug=true" for additional debug logging)
        "-sendMail" | "-sendmail" | "-sendEmail" | "-sendEMail" | "-sendemail" ) 
            if [ $# -ge 2 ]; then
                GTS_SENDMAIL="$2"
                shift
            else
                echo "Missing 'To' email specification on 'sendMail' option"
                exit 99
            fi
            shift
            ;;

        # - SMTP id (PrivateLabel name) 
        "-smtp" ) # "-smtpProperties"
            if [ $# -ge 2 ]; then
                GTS_SMTPPL="$2"
                shift
            else
                echo "Missing 'PrivateLabel' name specification on 'smtp' option"
                exit 99
            fi
            shift
            ;;

        # - send test SMS message 
        "-sendSMS" | "-sendsms" | "-sendSms" ) 
            if [ $# -ge 2 ]; then
                GTS_SENDSMS="$2"
                shift
            else
                echo "Missing Phone# specification on 'sendSMS' option"
                exit 99
            fi
            shift
            ;;

        # - show modified files (check file signatures)
        "-mods" | "-showMods" )
            GTS_FILEMODS=1
            shift
            ;;

        # - skip remaining arguments
        "--" )
            shift
            break # - exit case
            ;;

        # - error
        * )
        echo "Invalid option!  $1"
            #usage
            #exit 99
            break # - exit case
            ;;

    esac
done

# -----------------------------------------------------------------------------

# --- debug args
DEBUG_ARGS=""
if [ ${GTS_DEBUG} -ne 0 ]; then
    DEBUG_ARGS="-debugMode=true"
fi
if [ ${GTS_VERBOSE} -ne 0 ]; then
    DEBUG_ARGS="-rtverbose=true -debugMode=true"
fi

# --- assemble args
ARGS="-log.file.enable=false"
if [ "${GTS_SENDMAIL}" != "" ]; then
    if [ "${GTS_SMTPPL}" != "" ]; then
        ARGS="${ARGS} -smtp=${GTS_SMTPPL}"
    fi
    ARGS="${ARGS} -sendMail=${GTS_SENDMAIL}"
fi
if [ "${GTS_SENDSMS}" != "" ]; then
    ARGS="${ARGS} -sendSMS=${GTS_SENDSMS}"
fi
if [ ${GTS_FILEMODS} -ne 0 ]; then
    ARGS="${ARGS} -showMods"
fi

# --- memory
if [ "${JAVA_MEMORY}" = "" ]; then 
    JAVAMEM=-Xmx256m
else
    JAVAMEM=-Xmx${JAVA_MEMORY}
fi

# --- CheckInstall
CI_STATUS=0
JMAIN="org.opengts.tools.CheckInstall"
COMMAND="${CMD_JAVA} ${JAVAMEM} -Djava.awt.headless=true -classpath ${CPATH} ${JMAIN} '-conf=${GTS_CONF}' ${DEBUG_ARGS} ${ARGS} $1 $2 $3 $4"
if [ ${GTS_DEBUG} -ne 0 ]; then
    echo "${COMMAND}"
fi
if [ ${IS_WINDOWS} -eq 1 ]; then
    ${COMMAND}
    CI_STATUS=$?
else
    eval "${COMMAND}"
    CI_STATUS=$?
fi

# --- full check
if [ $FULL_CHECK -eq 1 ]; then
    printSep
    echo ""
    echo "Active Java System Processes/Applications:"
    if [ -x ${CMD_PSJAVA} ]; then
        ${CMD_PSJAVA}
    else
        echo ""
        echo "Command '${CMD_PSJAVA}' not found"
        echo ""
    fi
    printSep
    echo ""
    echo "Contents of GTS log directory [${LOG_DIR}]:"
    echo ""
    if [ -d ${LOG_DIR} ]; then
        ${CMD_LS} -l ${LOG_DIR}
    else
        echo "GTS log directory '${LOG_DIR}' not found"
    fi
    echo ""
    printSep
    echo ""
fi

# --- exit
exit $CI_STATUS

# ---
