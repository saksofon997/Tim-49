package com.project.tim49.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.project.tim49.dto.ClinicDTO;
import com.project.tim49.dto.RegistrationDTO;
import com.project.tim49.dto.UserDTO;
import com.project.tim49.model.*;
import com.project.tim49.repository.MedicalRecordRepository;
import com.project.tim49.repository.PatientRepository;
import com.project.tim49.repository.RegistrationRequestRepository;
import com.project.tim49.repository.UserRepository;
import com.project.tim49.service.AuthorityService;
import com.project.tim49.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.MethodNotAllowedException;

import javax.validation.ValidationException;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    @Lazy
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthorityService authService;
    @Autowired
    private RegistrationRequestRepository registrationRequestRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Override
    public User findByEmail(String email) throws UsernameNotFoundException {
        User u = userRepository.findByEmail(email);
        return u;
    }

    public User findById(Long id) throws AccessDeniedException {
        User u = userRepository.findById(id).orElseGet(null);
        return u;
    }

    public List<User> findAll() throws AccessDeniedException {
        List<User> result = userRepository.findAll();
        return result;
    }

    @Override
    public UserDTO createPatient(Long registration_request_id) {
        Optional<RegistrationRequest> request = registrationRequestRepository.findById(registration_request_id);

        if (!request.isPresent()) {
            throw new NoSuchElementException();
        }
        if (!request.get().isApproved()) {
            throw new SecurityException();
        }

        RegistrationRequest req = request.get();

        Patient patient = new Patient();

        patient.setEmail(req.getEmail());
        patient.setPassword(req.getPassword());
        patient.setName(req.getName());
        patient.setSurname(req.getSurname());
        patient.setAddress(req.getAddress());
        patient.setCity(req.getCity());
        patient.setState(req.getState());
        patient.setPhoneNumber(req.getPhoneNumber());
        patient.setUpin(req.getUpin());
        // izbaciti?
        patient.setEnabled(true);
        List<Authority> auth = authService.findByname("PATIENT");
        patient.setAuthorities(auth);
        patient.setPasswordChanged(true);

        MedicalRecord medicalRecord = new MedicalRecord();
        MedicalRecord saved = medicalRecordRepository.save(medicalRecord);
        patient.setMedicalRecord(saved);

        patient = patientRepository.save(patient);
        registrationRequestRepository.delete(req);

        // patient DTO
        UserDTO patientDto = new UserDTO(patient);
        return patientDto;
    }

    public RegistrationDTO createRegistrationRequest(RegistrationDTO registrationDTO) {

        RegistrationRequest request = this.registrationRequestRepository.findOneByEmail(registrationDTO.getEmail());
        if (request == null) {
            request = new RegistrationRequest();

            request.setEmail(registrationDTO.getEmail());
            request.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
            request.setName(registrationDTO.getName());
            request.setSurname(registrationDTO.getSurname());
            request.setAddress(registrationDTO.getAddress());
            request.setCity(registrationDTO.getCity());
            request.setState(registrationDTO.getState());
            request.setPhoneNumber(registrationDTO.getPhoneNumber());
            request.setUpin(registrationDTO.getUpin());
            request.setApproved(false);

            RegistrationRequest saved = this.registrationRequestRepository.save(request);
            return new RegistrationDTO(saved);
        }
        throw new ValidationException("Registration with this e-mail already " +
                "exists!");
    }

    @Override
    public List<RegistrationDTO> getRegistrationRequests() {
        List<RegistrationDTO> registrationDTOS = new ArrayList<>();
        List<RegistrationRequest> registrationRequests = this.registrationRequestRepository.findAll();
        for (RegistrationRequest rreq : registrationRequests) {
            if (!rreq.isApproved()) {
                registrationDTOS.add(new RegistrationDTO(rreq));
            }
        }
        return registrationDTOS;
    }

    // readOnly = false -- modifikujemo registration request
    // propagation = requires_new -- za svaki poziv metode se pokrece nova transakcija
    @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
    @Override
    public RegistrationDTO approveRegistrationRequest(RegistrationDTO registrationDTO) {
        RegistrationRequest request = this.registrationRequestRepository.findOneByEmail(registrationDTO.getEmail());
        if (request == null) {
            throw new ValidationException("Registration request with provided e-mail does not exist!");
        }
        if (request.isApproved()){
            throw new ValidationException("Registration request has already been approved!");
        }
        request.setApproved(true);
        RegistrationRequest saved=this.registrationRequestRepository.save(request);
        // Za testiranje konkurentnog pristupa
        // try { Thread.sleep(5000); } catch (InterruptedException e) { }
        return new RegistrationDTO(saved);
    }

    // readOnly = false -- modifikujemo vacation
    // propagation = requires_new -- za svaki poziv metode se pokrece nova transakcija
    @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
    @Override
    public RegistrationDTO deleteRegistrationRequest(Long id) {
        Optional<RegistrationRequest> request = registrationRequestRepository.findById(id);
        if (!request.isPresent()) {
            throw new NoSuchElementException();
        }
        if (request.get().isApproved()) {
            throw new SecurityException();
        }

        RegistrationRequest req = request.get();
        registrationRequestRepository.delete(req);
        RegistrationDTO requestDto = new RegistrationDTO(req);
        return requestDto;
    }

}
