package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.Data;

/**
 * Terms metadata service connection and authentication options.
 */
@Data
public class TermsMetadataOptions {

    private String password;
    private String userkeysFile;
    private String source = "ae-kosmos";
}
