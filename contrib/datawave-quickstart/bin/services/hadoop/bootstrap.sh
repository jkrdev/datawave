# Sourced by env.sh

DW_HADOOP_SERVICE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# You may override DW_HADOOP_DIST_URI in your env ahead of time, and set as file:///path/to/file.tar.gz for local tarball, if needed
DW_HADOOP_DIST_URI="${DW_HADOOP_DIST_URI:-http://mirrors.advancedhosters.com/apache/hadoop/common/hadoop-3.1.1/hadoop-3.1.1.tar.gz}"
DW_HADOOP_DIST="$( downloadTarball "${DW_HADOOP_DIST_URI}" "${DW_HADOOP_SERVICE_DIR}" && echo "${tarball}" )"
DW_HADOOP_BASEDIR="hadoop-install"
DW_HADOOP_SYMLINK="hadoop"

DW_HADOOP_DFS_URI="hdfs://localhost:9000"
DW_HADOOP_MR_INTER_DIR="${DW_CLOUD_DATA}/hadoop/jobhist/inter"
DW_HADOOP_MR_DONE_DIR="${DW_CLOUD_DATA}/hadoop/jobhist/done"
DW_HADOOP_RESOURCE_MANAGER_ADDRESS="localhost:8050"

HADOOP_HOME="${DW_CLOUD_HOME}/${DW_HADOOP_SYMLINK}"

# core-site.xml (Format: <property-name><space><property-value>{<newline>})
DW_HADOOP_CORE_SITE_CONF="fs.defaultFS ${DW_HADOOP_DFS_URI}
hadoop.tmp.dir file:/${DW_CLOUD_DATA}/hadoop/tmp
io.compression.codecs org.apache.hadoop.io.compress.GzipCodec"

# hdfs-site.xml (Format: <property-name><space><property-value>{<newline>})
DW_HADOOP_HDFS_SITE_CONF="dfs.namenode.name.dir file://${DW_CLOUD_DATA}/hadoop/nn
dfs.namenode.checkpoint.dir file://${DW_CLOUD_DATA}/hadoop/nnchk
dfs.datanode.data.dir file://${DW_CLOUD_DATA}/hadoop/dn
dfs.datanode.handler.count 10
dfs.datanode.synconclose true
dfs.replication 1"

DW_HADOOP_MR_HEAPDUMP_DIR="${DW_CLOUD_DATA}/heapdumps"
# mapred-site.xml (Format: <property-name><space><property-value>{<newline>})
DW_HADOOP_MAPRED_SITE_CONF="mapreduce.jobhistory.address http://localhost:8020
mapreduce.jobhistory.webapp.address http://localhost:8021
mapreduce.jobhistory.intermediate-done-dir ${DW_HADOOP_MR_INTER_DIR}
mapreduce.jobhistory.done-dir ${DW_HADOOP_MR_DONE_DIR}
mapreduce.map.memory.mb 2048
mapreduce.reduce.memory.mb 2048
mapreduce.map.java.opts -Xmx1024m -server -XX:NewRatio=8 -Djava.net.preferIPv4Stack=true -XX:+ExitOnOutOfMemoryError -XX:HeapDumpPath=${DW_HADOOP_MR_HEAPDUMP_DIR}
mapreduce.reduce.java.opts -Xmx1792m -server -XX:NewRatio=8 -Djava.net.preferIPv4Stack=true -XX:+ExitOnOutOfMemoryError -XX:HeapDumpPath=${DW_HADOOP_MR_HEAPDUMP_DIR}
mapreduce.framework.name yarn
yarn.app.mapreduce.am.env HADOOP_MAPRED_HOME=${HADOOP_HOME}
mapreduce.map.env HADOOP_MAPRED_HOME=${HADOOP_HOME}
mapreduce.reduce.env HADOOP_MAPRED_HOME=${HADOOP_HOME}"

# yarn-site.xml (Format: <property-name><space><property-value>{<newline>})
DW_HADOOP_YARN_SITE_CONF="yarn.resourcemanager.scheduler.address localhost:8030
yarn.resourcemanager.scheduler.class org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler
yarn.resourcemanager.resource-tracker.address localhost:8025
yarn.resourcemanager.address ${DW_HADOOP_RESOURCE_MANAGER_ADDRESS}
yarn.resourcemanager.admin.address localhost:8033
yarn.resourcemanager.webapp.address localhost:8088
yarn.nodemanager.local-dirs ${DW_CLOUD_DATA}/hadoop/yarn/local
yarn.nodemanager.log-dirs ${DW_CLOUD_DATA}/hadoop/yarn/log
yarn.nodemanager.aux-services mapreduce_shuffle
yarn.nodemanager.pmem-check-enabled false
yarn.nodemanager.vmem-check-enabled false
yarn.nodemanager.resource.memory-mb 4096
yarn.app.mapreduce.am.resource.mb 1024
yarn.log.server.url http://localhost:8070/jobhistory/logs"

# capacity-scheduler.xml (Format: <property-name><space><property-value>{<newline>})
DW_HADOOP_CAPACITY_SCHEDULER_CONF="yarn.scheduler.capacity.maximum-applications 10000
yarn.scheduler.capacity.maximum-am-resource-percent 0.1
yarn.scheduler.capacity.resource-calculator org.apache.hadoop.yarn.util.resource.DefaultResourceCalculator
yarn.scheduler.capacity.root.queues default,bulkIngestQueue,liveIngestQueue
yarn.scheduler.capacity.root.default.capacity 20
yarn.scheduler.capacity.root.bulkIngestQueue.capacity 40
yarn.scheduler.capacity.root.liveIngestQueue.capacity 40
yarn.scheduler.capacity.root.default.user-limit-factor 0.2
yarn.scheduler.capacity.root.bulkIngestQueue.user-limit-factor 0.4
yarn.scheduler.capacity.root.liveIngestQueue.user-limit-factor 0.4
yarn.scheduler.capacity.root.default.maximum-capacity 100
yarn.scheduler.capacity.root.bulkIngestQueue.maximum-capacity 90
yarn.scheduler.capacity.root.liveIngestQueue.maximum-capacity 90
yarn.scheduler.capacity.root.default.state RUNNING
yarn.scheduler.capacity.root.bulkIngestQueue.state RUNNING
yarn.scheduler.capacity.root.liveIngestQueue.state RUNNING
yarn.scheduler.capacity.root.default.acl_submit_applications *
yarn.scheduler.capacity.root.default.acl_administer_queue *
yarn.scheduler.capacity.node-locality-delay 40"

# Hadoop standard exports...
export HADOOP_HOME
export HADOOP_CONF_DIR="${HADOOP_HOME}/etc/hadoop"
export HADOOP_LOG_DIR="${HADOOP_HOME}/logs"
export HADOOP_YARN_HOME="${HADOOP_HOME}"
export HADOOP_MAPRED_HOME="${HADOOP_HOME}"
export HADOOP_PID_DIR="${DW_CLOUD_DATA}/hadoop/pids"

export PATH=${HADOOP_HOME}/bin:$PATH

# Service helpers...

DW_HADOOP_CMD_START="( cd ${HADOOP_HOME}/sbin && ./start-dfs.sh && ./start-yarn.sh && mapred --daemon start historyserver )"
DW_HADOOP_CMD_STOP="( cd ${HADOOP_HOME}/sbin && mapred --daemon stop historyserver && ./stop-yarn.sh && ./stop-dfs.sh )"
DW_HADOOP_CMD_FIND_ALL_PIDS="pgrep -f 'datanode.DataNode|namenode.NameNode|namenode.SecondaryNameNode|nodemanager.NodeManager|resourcemanager.ResourceManager|mapreduce.v2.hs.JobHistoryServer'"

function hadoopIsRunning() {
    DW_HADOOP_PID_LIST="$(eval "${DW_HADOOP_CMD_FIND_ALL_PIDS} -d ' '")"
    [ -z "${DW_HADOOP_PID_LIST}" ] && return 1 || return 0
}

function hadoopStart() {
    hadoopIsRunning && echo "Hadoop is already running" || eval "${DW_HADOOP_CMD_START}"
    echo
    info "For detailed status visit 'http://localhost:50070/dfshealth.html#tab-overview' in your browser"
    # Wait for Hadoop to come out of safemode
    ${HADOOP_HOME}/bin/hdfs dfsadmin -safemode wait
}

function hadoopStop() {
    hadoopIsRunning && eval "${DW_HADOOP_CMD_STOP}" || echo "Hadoop is already stopped"
}

function hadoopStatus() {
    # define local variables for hadoop processes
    local _jobHist
    local _dataNode
    local _nameNode
    local _secNameNode
    local _resourceMgr
    local _nodeMgr

    # use a state to parse jps entries
    echo "======  Hadoop Status  ======"
    hadoopIsRunning && {
        local _pid
        local _opt=pid

        local -r _pids=${DW_HADOOP_PID_LIST// /|}
        echo "pids: ${DW_HADOOP_PID_LIST}"
        for _arg in $(jps -l | egrep "${_pids}"); do
            case ${_opt} in
                pid)
                    _pid=${_arg}
                    _opt=class
                    ;;
                class)
                    local _none
                    local _name=${_arg##*.}
                    case "${_name}" in
                        DataNode) _dataNode=${_pid};;
                        JobHistoryServer) _jobHist=${_pid};;
                        NameNode) _nameNode=${_pid};;
                        NodeManager) _nodeMgr=${_pid};;
                        ResourceManager) _resourceMgr=${_pid};;
                        SecondaryNameNode) _secNameNode=${_pid};;
                        *) _none=true;;
                    esac
                    test -z "${_none}" && info "${_name} => ${_pid}"
                    _pid=
                    _opt=pid
                    unset _none
                    ;;
            esac
        done
    }

    test -z "${_jobHist}" && error "hadoop job history is not running"
    test -z "${_dataNode}" && error "hadoop data node is not running"
    test -z "${_nameNode}" && error "hadoop name node is not running"
    test -z "${_secNameNode}" && error "hadoop secondary name node is not running"
    test -z "${_nodeMgr}" && error "hadoop node manager is not running"
    test -z "${_resourceMgr}" && error "hadoop resource maanger is not running"
}

function hadoopIsInstalled() {
    [ -L "${DW_CLOUD_HOME}/${DW_HADOOP_SYMLINK}" ] && return 0
    [ -d "${DW_HADOOP_SERVICE_DIR}/${DW_HADOOP_BASEDIR}" ] && return 0
    return 1
}

function hadoopUninstall() {
   if hadoopIsInstalled ; then
      if [ -L "${DW_CLOUD_HOME}/${DW_HADOOP_SYMLINK}" ] ; then
          ( cd "${DW_CLOUD_HOME}" && unlink "${DW_HADOOP_SYMLINK}" ) || error "Failed to remove Hadoop symlink"
      fi

      if [ -d "${DW_HADOOP_SERVICE_DIR}/${DW_HADOOP_BASEDIR}" ] ; then
          rm -rf "${DW_HADOOP_SERVICE_DIR}/${DW_HADOOP_BASEDIR}"
      fi

      ! hadoopIsInstalled && info "Hadoop uninstalled" || error "Failed to uninstall Hadoop"
   else
      info "Hadoop not installed. Nothing to do"
   fi

   [[ "${1}" == "${DW_UNINSTALL_RM_BINARIES_FLAG_LONG}" || "${1}" == "${DW_UNINSTALL_RM_BINARIES_FLAG_SHORT}" ]] && rm -f "${DW_HADOOP_SERVICE_DIR}"/*.tar.gz
}

function hadoopInstall() {
   "${DW_HADOOP_SERVICE_DIR}"/install.sh
}

function hadoopPrintenv() {
   echo
   echo "Hadoop Environment"
   echo
   ( set -o posix ; set ) | grep HADOOP_
   echo
}

function hadoopPidList() {

   hadoopIsRunning && echo "${DW_HADOOP_PID_LIST}"

}
