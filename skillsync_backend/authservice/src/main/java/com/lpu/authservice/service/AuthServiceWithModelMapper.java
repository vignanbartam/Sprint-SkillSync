package com.lpu.authservice.service;

import com.lpu.authservice.dto.UserDTO;
import com.lpu.authservice.entity.User;
import com.lpu.authservice.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *  EXAMPLE: Shows ModelMapper usage in place of manual mapping.
 * Replace manual "new UserDTO(user.getId(), user.getEmail())" style
 * with modelMapper.map(user, UserDTO.class)
 */
@Service
public class AuthServiceWithModelMapper {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ModelMapper modelMapper;   // ← injected via ModelMapperConfig bean

    public UserDTO getUserDTO(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        //  Automatic field-to-field mapping (id, email)
        return modelMapper.map(user, UserDTO.class);
    }
}
