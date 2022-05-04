package com.apixio.model.external;

import java.util.*;

public class AxmProviderInRole {
    private AxmExternalId providerId;
    private AxmActorRole actorRole;

    public void setProviderId(AxmExternalId providerId) {
        this.providerId = providerId;
    }

    public AxmExternalId getProviderId() {
        return providerId;
    }

    public void setActorRole(AxmActorRole actorRole) {
        this.actorRole = actorRole;
    }

    public AxmActorRole getActorRole() {
        return actorRole;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(providerId, actorRole);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        if (this.getClass() != obj.getClass()) return false;

        final AxmProviderInRole that = (AxmProviderInRole) obj;
        return Objects.equals(this.providerId, that.providerId)
            && Objects.equals(this.actorRole, that.actorRole);
    }
}
