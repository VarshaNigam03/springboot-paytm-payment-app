package com.paytmpayment.springbootpaytmpaymentapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import com.paytm.pg.merchant.PaytmChecksum;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.TreeMap;

@Controller
public class PaymentController {

    @Autowired
    private PaytmDetailPojo paytmDetailPojo;

    //to access or use values from properties values/file
    @Autowired
    private Environment env;

    //takes to home page
    @GetMapping("/")
    public String home(){
        return "home";
    }

    @PostMapping(value = "/submitPaymentDetail")
    public ModelAndView getRedirect(@RequestParam(name = "CUST_ID") String customerId,
                                    @RequestParam(name = "TXN_AMOUNT") String transactionAmount,
                                    @RequestParam(name = "ORDER_ID") String orderId) throws Exception{

        //this will load pojo class for url and go to properties file to get the url, details
        ModelAndView modelAndView = new ModelAndView("redirect: "+paytmDetailPojo.getPaytmUrl());
        TreeMap<String, String> parameters = new TreeMap<>();
        paytmDetailPojo.getDetails().forEach((k, v) -> parameters.put(k, v));
        parameters.put("MOBILE_NO", env.getProperty("paytm.mobile"));
        parameters.put("EMAIL", env.getProperty("paytm.email"));
        parameters.put("TXN_AMOUNT", transactionAmount);
        parameters.put("ORDER_ID", orderId);
        parameters.put("CUST_ID", customerId);
        String checkSum = getCheckSum(parameters);//validate
        parameters.put("CHECKSUMHASH", checkSum);
        modelAndView.addAllObjects(parameters);
        return modelAndView;
    }

    @PostMapping(value = "/pgresponse")
    public String getResponseRedirect(HttpServletRequest request, Model model){
        Map<String, String[]> mapData = request.getParameterMap();
        TreeMap<String, String> parameters = new TreeMap<String, String>();
        String paytmChecksum = "";
        for(Map.Entry<String, String[]> requestParamsEntry : mapData.entrySet()){
            if("CHECKSUMHASH".equalsIgnoreCase(requestParamsEntry.getKey())){
                paytmChecksum = requestParamsEntry.getValue()[0];
            }
            else{
                parameters.put(requestParamsEntry.getKey(), requestParamsEntry.getValue()[0]);
            }
        }
        String result;

        boolean isValideCheckSum = false;
        System.out.println("RESULT : "+parameters.toString());
        try{
            isValideCheckSum = validateCheckSum(parameters, paytmChecksum);
            if(isValideCheckSum && parameters.containsKey("RESPCODE")){
                if(parameters.get("RESPCODE").equals("01")){
                    result = "Payment successful";
                }
                else{
                    result = "Payment failed";
                }
            }
                else{
                    result = "Checksum mismatched";
                }
            }
        catch(Exception e){
            result = e.toString();
        }
        model.addAttribute("result", result);
        parameters.remove("CHECKSUMHASH");
        model.addAttribute("paramters",parameters);
        return "report";
    }

    private boolean validateCheckSum(TreeMap<String, String> parameters, String paytmChecksum) throws Exception{
        return PaytmChecksum.verifySignature(parameters, paytmDetailPojo.getMerchantKey(),paytmChecksum);
    }

    //
    private String getCheckSum(TreeMap<String, String> parameters) throws Exception{
        return  PaytmChecksum.generateSignature(parameters, paytmDetailPojo.getMerchantKey());
    }

}
