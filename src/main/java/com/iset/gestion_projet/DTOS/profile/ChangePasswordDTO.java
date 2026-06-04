package com.iset.gestion_projet.DTOS.profile;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordDTO {

    private String currentPassword;
    private String newPassword;
}