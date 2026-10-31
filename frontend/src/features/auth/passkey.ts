import {
    startAuthentication,
    startRegistration,
    WebAuthnError,
    type PublicKeyCredentialCreationOptionsJSON,
    type PublicKeyCredentialRequestOptionsJSON,
} from '@simplewebauthn/browser';
import type {
    PasskeyAuthenticationOptionsDto,
    PasskeyDescriptorDto,
    PasskeyRegistrationOptionsDto,
} from '../../api/generated.ts';

function descriptor(d: PasskeyDescriptorDto) {
    return {
        id: d.id,
        type: 'public-key' as const,
        transports: d.transports,
    };
}

function requestOptions(dto: PasskeyAuthenticationOptionsDto): PublicKeyCredentialRequestOptionsJSON {
    return {
        challenge: dto.challenge,
        rpId: dto.rpId,
        timeout: dto.timeout,
        userVerification: dto.userVerification as UserVerificationRequirement,
        allowCredentials: dto.allowCredentials.map(descriptor),
    };
}

function creationOptions(dto: PasskeyRegistrationOptionsDto): PublicKeyCredentialCreationOptionsJSON {
    return {
        rp: dto.rp,
        user: dto.user,
        challenge: dto.challenge,
        pubKeyCredParams: dto.pubKeyCredParams.map((p) => ({ type: 'public-key' as const, alg: p.alg ?? -7 })),
        timeout: dto.timeout,
        excludeCredentials: dto.excludeCredentials.map(descriptor),
        authenticatorSelection: {
            residentKey: dto.authenticatorSelection.residentKey as ResidentKeyRequirement,
            requireResidentKey: dto.authenticatorSelection.requireResidentKey,
            userVerification: dto.authenticatorSelection.userVerification as UserVerificationRequirement,
        },
        attestation: dto.attestation as AttestationConveyancePreference,
    };
}

/** Runs the browser's passkey prompt; returns the JSON the backend expects as `credentialJson`. */
export async function getPasskeyAssertion(dto: PasskeyAuthenticationOptionsDto, autofill = false): Promise<string> {
    const response = await startAuthentication({ optionsJSON: requestOptions(dto), useBrowserAutofill: autofill });
    return JSON.stringify(response);
}

export async function createPasskey(dto: PasskeyRegistrationOptionsDto): Promise<string> {
    const response = await startRegistration({ optionsJSON: creationOptions(dto) });
    return JSON.stringify(response);
}

/** The user closed the prompt, or another ceremony replaced this one - nothing to report. */
export function isPasskeyCancelled(error: unknown): boolean {
    if (error instanceof WebAuthnError && error.code === 'ERROR_CEREMONY_ABORTED') return true;
    const name = (error as { name?: string } | null)?.name;
    return name === 'NotAllowedError' || name === 'AbortError';
}
