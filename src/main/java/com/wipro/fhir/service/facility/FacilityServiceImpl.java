package com.wipro.fhir.service.facility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wipro.fhir.data.mongo.care_context.AddCareContextRequest;
import com.wipro.fhir.data.mongo.care_context.SaveFacilityIdForVisit;
import com.wipro.fhir.repo.healthID.BenHealthIDMappingRepo;
import com.wipro.fhir.service.ndhm.Common_NDHMService;
import com.wipro.fhir.service.ndhm.GenerateSession_NDHMService;
import com.wipro.fhir.utils.exception.FHIRException;
import com.wipro.fhir.utils.http.HttpUtils;
import com.wipro.fhir.utils.mapper.InputMapper;
@Service
public class FacilityServiceImpl implements FacilityService{
	
	@Value("${getAbdmFacilityServicies}")
	private String getAbdmServicies;
	
	@Value("${abdmFacilityId}")
	private String abdmFacilityId;
	
	@Autowired
	private HttpUtils httpUtils;

	@Autowired
	private Common_NDHMService common_NDHMService;
	
	@Autowired
	private GenerateSession_NDHMService generateSession_NDHM;
	
	@Autowired
	private BenHealthIDMappingRepo benHealthIDMappingRepo;

	@Override
	public String fetchRegisteredFacilities() throws FHIRException {
		String res = null;
		List<HashMap<String,Object>> list = new ArrayList<>();
		HashMap<String,Object> map = new HashMap<>();
		try {
			String ndhmAuthToken = generateSession_NDHM.getNDHMAuthToken();
			HttpHeaders headers = common_NDHMService.getHeaders(ndhmAuthToken);
			ResponseEntity<String> responseEntity = httpUtils.getWithResponseEntity(getAbdmServicies, headers);
			String responseStrLogin = common_NDHMService.getBody(responseEntity);
			if (responseStrLogin != null) {
				JsonObject jsnOBJ = new JsonObject();
				JsonParser jsnParser = new JsonParser();
				JsonElement jsnElmnt = jsnParser.parse(responseStrLogin);
				jsnOBJ = jsnElmnt.getAsJsonObject();
				JsonElement jsonElement = jsnOBJ.get("services");
				JsonArray asJsonArray = jsonElement.getAsJsonArray();
				for (JsonElement jsonElement2 : asJsonArray) {
					JsonObject asJsonObject = jsonElement2.getAsJsonObject();
					String types = asJsonObject.get("types").toString();
					if(null != types && types.contains("HIP")) {
						map = new HashMap<>();
						map.put("id", asJsonObject.get("id"));
						map.put("name", asJsonObject.get("name"));
						list.add(map);
					}
				}
				res = new Gson().toJson(list);
			} else
				res = "NDHM_FHIR Error while getting facilities";
		}
			
		catch (Exception e) {
			throw new FHIRException("NDHM_FHIR Error while accessing ABDM API" + e.getMessage());
		}
		return res;
	}
	
	@Override
	public String saveAbdmFacilityId(String reqObj) throws FHIRException {
		String res = null;
		try {
			SaveFacilityIdForVisit requestObj = InputMapper.gson().fromJson(reqObj, SaveFacilityIdForVisit.class);
			if(requestObj.getFacilityId() == null || requestObj.getFacilityId() == "") {
				requestObj.setFacilityId(abdmFacilityId);
			}
			Integer response = benHealthIDMappingRepo.updateFacilityIdForVisit(requestObj.getVisitCode(), requestObj.getFacilityId());
			if(response > 0 ) {
				res = "ABDM Facility ID updated successfully";
			} else
				res = "FHIR Error while updating ABDM Facility ID";
		}
		catch (Exception e) {
			throw new FHIRException(e.getMessage());
		}
		return res;
	}
	
}
  