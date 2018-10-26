package com.application.base.dubbo.monitor.controller;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.application.base.dubbo.monitor.RegistryContainer;
import com.application.base.dubbo.monitor.entity.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ServicesController
 *
 *@author admin
 */
@Controller
@RequestMapping("/services")
public class ServicesController {

    @Autowired
    private RegistryContainer registryContainer;

    @RequestMapping(method = RequestMethod.GET)
    public String services(Model model) {
        List<DubboService> rows = new ArrayList<DubboService>();
        Set<String> services = registryContainer.getServices();

        if (services != null && services.size() > 0) {
            DubboService dubboService;
            for (String service : services) {
                dubboService = new DubboService();
                dubboService.setName(service);

                List<URL> providers = registryContainer.getProvidersByService(service);
                int providerSize = providers == null ? 0 : providers.size();
                dubboService.setProviderCount(providerSize);

                List<URL> consumers = registryContainer.getConsumersByService(service);
                int consumerSize = consumers == null ? 0 : consumers.size();
                dubboService.setConsumerCount(consumerSize);

                if (providerSize > 0) {
                    URL provider = providers.iterator().next();
                    dubboService.setApplication(provider.getParameter(Constants.APPLICATION_KEY, ""));
                    dubboService.setOwner(provider.getParameter("owner", ""));
                    dubboService.setOrganization((provider.hasParameter("organization") ? provider.getParameter("organization") : ""));
                }

                rows.add(dubboService);
            }
        }

        model.addAttribute("rows", rows);
        return "service/services";
    }

    @RequestMapping(value = "/providers", method = RequestMethod.GET)
    public String providers(@RequestParam String service, Model model) {
        List<URL> providers = registryContainer.getProvidersByService(service);
        List<String> rows = new ArrayList<String>();
        if (providers != null && providers.size() > 0) {
            for (URL u : providers) {
                rows.add(u.toFullString());
            }
        }

        model.addAttribute("service", service);
        model.addAttribute("rows", rows);
        return "service/providers";
    }

    @RequestMapping(value = "/consumers", method = RequestMethod.GET)
    public String consumers(@RequestParam String service, Model model) {
        List<URL> consumers = registryContainer.getConsumersByService(service);
        List<String> rows = new ArrayList<String>();
        if (consumers != null && consumers.size() > 0) {
            for (URL u : consumers) {
                rows.add(u.toFullString());
            }
        }

        model.addAttribute("service", service);
        model.addAttribute("rows", rows);
        return "service/consumers";
    }
}
