/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.itests.basic.examples;


import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerCondition;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
import io.fabric8.zookeeper.ZkPath;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ExampleCamelProfileTest extends FabricTestSupport {

    @Test
    public void testExample() throws Exception {
        System.out.println(executeCommand("fabric:create -n"));
        System.out.println(executeCommand("fabric:profile-list"));

        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            CuratorFramework curator = fabricService.adapt(CuratorFramework.class);

            Set<Container> containers = ContainerBuilder.create(fabricProxy, 2).withName("cnt").withProfiles("default").assertProvisioningResult().build();
            try {
                LinkedList<Container> containerList = new LinkedList<Container>(containers);
                Container broker = containerList.removeLast();

                setData(curator, ZkPath.CONTAINER_PROVISION_RESULT.getPath(broker.getId()), "changing");
                System.out.println(executeCommand("fabric:container-change-profile " + broker.getId() + " mq-default"));
                Provision.provisioningSuccess(Arrays.asList(new Container[]{broker}), PROVISION_TIMEOUT);
                System.out.println(executeCommand("fabric:cluster-list"));

                for(Container c : containerList) {
                    setData(curator, ZkPath.CONTAINER_PROVISION_RESULT.getPath(c.getId()), "changing");
                    System.out.println(executeCommand("fabric:container-change-profile " + c.getId() + " example-camel-mq"));
                }
                Provision.provisioningSuccess(containerList, PROVISION_TIMEOUT);

                Assert.assertTrue(Provision.waitForCondition(containerList, new ContainerCondition() {
                    @Override
                    public Boolean checkConditionOnContainer(final Container c) {
                        System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + c.getId() + " osgi:list"));
                        System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + c.getId() + " camel:route-list"));
                        String completed = executeCommand("fabric:container-connect -u admin -p admin " + c.getId() + " camel:route-info route2 | grep \"Exchanges Completed\"");
                        System.out.println(completed);
                        if (completed.contains("Exchanges Completed") && !completed.contains("Exchanges Completed: 0")) {
                            return true;
                        } else {
                            return false;
                        }

                    }
                }, 10000L));
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                //debugConfiguration("5005",false)
        };
    }
}
