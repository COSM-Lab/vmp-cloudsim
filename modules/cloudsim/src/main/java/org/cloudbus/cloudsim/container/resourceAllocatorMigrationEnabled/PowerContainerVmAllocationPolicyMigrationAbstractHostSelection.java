package org.cloudbus.cloudsim.container.resourceAllocatorMigrationEnabled;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.container.core.*;
import org.cloudbus.cloudsim.container.hostSelectionPolicies.HostSelectionPolicy;
import org.cloudbus.cloudsim.container.vmSelectionPolicies.PowerContainerVmSelectionPolicy;
import org.cloudbus.cloudsim.lists.HostList;
import org.cloudbus.cloudsim.power.PowerHost;

import java.util.*;

/**
 * Created by sareh on 17/11/15.
 * Modified by Remo Andreoli (Feb 2024)
 */
public class PowerContainerVmAllocationPolicyMigrationAbstractHostSelection extends PowerContainerVmAllocationPolicyMigrationAbstract {

    private HostSelectionPolicy hostSelectionPolicy;
    private double utilizationThreshold = 0.9;
    private double underUtilizationThreshold = 0.7;

    /**
     * Instantiates a new power vm allocation policy migration abstract.
     *
     * @param hostSelectionPolicy
     * @param hostList            the host list
     * @param vmSelectionPolicy   the vm selection policy
     */
    public PowerContainerVmAllocationPolicyMigrationAbstractHostSelection(List<? extends Host> hostList, PowerContainerVmSelectionPolicy vmSelectionPolicy, HostSelectionPolicy hostSelectionPolicy, double OlThreshold, double UlThreshold) {
        super(hostList, vmSelectionPolicy);
        setHostSelectionPolicy(hostSelectionPolicy);
        setUtilizationThreshold(OlThreshold);
        setUnderUtilizationThreshold(UlThreshold);
    }

    @Override
    /**
     * Find host for vm.
     *
     * @param vm            the vm
     * @param excludedHosts the excluded hosts
     * @return the power host
     */
    public PowerHost findHostForVm(ContainerVm vm, Set<? extends Host> excludedHosts) {
        PowerHost allocatedHost = null;
        boolean find = false;
        Set<Host> excludedHost1 = new HashSet<>(excludedHosts);
        while (!find) {
            Host host = getHostSelectionPolicy().getHost(getHostList(), vm, excludedHost1);
            if (host == null) {
                return allocatedHost;
            }
            if (host.isSuitableForVm(vm)) {
                find = true;
                allocatedHost = (PowerHost) host;
            } else {
                excludedHost1.add(host);
                if (getHostList().size() == excludedHost1.size()) {

                    return null;

                }
            }

        }
        return allocatedHost;
    }


    public HostSelectionPolicy getHostSelectionPolicy() {
        return hostSelectionPolicy;
    }

    public void setHostSelectionPolicy(HostSelectionPolicy hostSelectionPolicy) {
        this.hostSelectionPolicy = hostSelectionPolicy;
    }


    /**
     * Checks if is host over utilized.
     *
     * @param host the _host
     * @return true, if is host over utilized
     */
    @Override
    protected boolean isHostOverUtilized(PowerHost host) {
        addHistoryEntry(host, getUtilizationThreshold());
        double totalRequestedMips = 0;
        for (ContainerVm vm : host.<ContainerVm>getVmList()) {
            totalRequestedMips += vm.getCurrentRequestedTotalMips();
        }
        double utilization = totalRequestedMips / host.getTotalMips();
        return utilization > getUtilizationThreshold();
    }

    @Override
    protected boolean isHostUnderUtilized(PowerHost host) {
        return false;
    }

    /**
     * Sets the utilization threshold.
     *
     * @param utilizationThreshold the new utilization threshold
     */
    protected void setUtilizationThreshold(double utilizationThreshold) {
        this.utilizationThreshold = utilizationThreshold;
    }

    /**
     * Gets the utilization threshold.
     *
     * @return the utilization threshold
     */
    protected double getUtilizationThreshold() {
        return utilizationThreshold;
    }

    public double getUnderUtilizationThreshold() {
        return underUtilizationThreshold;
    }

    public void setUnderUtilizationThreshold(double underUtilizationThreshold) {
        this.underUtilizationThreshold = underUtilizationThreshold;
    }


    @Override
    /**
     * Gets the under utilized host.
     *Checks if the utilization is under the threshold then counts it as underUtilized :)
     * @param excludedHosts the excluded hosts
     * @return the under utilized host
     */
    protected PowerHost getUnderUtilizedHost(Set<? extends Host> excludedHosts) {

        List<Host> underUtilizedHostList = getUnderUtilizedHostList(excludedHosts);
        if (underUtilizedHostList.size() == 0) {

            return null;
        }
        HostList.sortByCpuUtilizationDescending(underUtilizedHostList);
//        Log.print(String.format("The under Utilized Hosts are %d", underUtilizedHostList.size()));
        PowerHost underUtilizedHost = (PowerHost) underUtilizedHostList.get(0);

        return underUtilizedHost;
    }


    /**
     * Gets the under utilized host.
     *
     * @param excludedHosts the excluded hosts
     * @return the under utilized host
     */
    protected List<Host> getUnderUtilizedHostList(Set<? extends Host> excludedHosts) {
        List<Host> underUtilizedHostList = new ArrayList<>();
        for (PowerHost host : this.<PowerHost>getHostList()) {
            if (excludedHosts.contains(host)) {
                continue;
            }
            double utilization = host.getUtilizationOfCpu();
            if (!areAllVmsMigratingOutOrAnyVmMigratingIn(host) && utilization < getUnderUtilizationThreshold() && !areAllContainersMigratingOutOrAnyContainersMigratingIn(host)) {
                underUtilizedHostList.add(host);
            }
        }
        return underUtilizedHostList;
    }
}