package com.serotonin.bacnet4j.service.confirmed;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.ObjectProperties;
import com.serotonin.bacnet4j.obj.PropertyTypeDefinition;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BACnetError;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ChangeListError;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.util.queue.ByteQueue;

public class RemoveListElementRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 9;
    
    private final ObjectIdentifier objectIdentifier;
    private final PropertyIdentifier propertyIdentifier;
    private final UnsignedInteger propertyArrayIndex;
    private final SequenceOf<? extends Encodable> listOfElements;
    
    public RemoveListElementRequest(ObjectIdentifier objectIdentifier, PropertyIdentifier propertyIdentifier, 
            UnsignedInteger propertyArrayIndex, SequenceOf<? extends Encodable> listOfElements) {
        this.objectIdentifier = objectIdentifier;
        this.propertyIdentifier = propertyIdentifier;
        this.propertyArrayIndex = propertyArrayIndex;
        this.listOfElements = listOfElements;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }
    
    @Override
    public void write(ByteQueue queue) {
        write(queue, objectIdentifier, 0);
        write(queue, propertyIdentifier, 1);
        writeOptional(queue, propertyArrayIndex, 2);
        write(queue, listOfElements, 3);
    }
    
    RemoveListElementRequest(ByteQueue queue) throws BACnetException {
        objectIdentifier = read(queue, ObjectIdentifier.class, 0);
        propertyIdentifier = read(queue, PropertyIdentifier.class, 1);
        propertyArrayIndex = readOptional(queue, UnsignedInteger.class, 2);
        PropertyTypeDefinition def = ObjectProperties.getPropertyTypeDefinition(
                objectIdentifier.getObjectType(), propertyIdentifier);
        listOfElements = readSequenceOf(queue, def.getClazz(), 3);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AcknowledgementService handle(LocalDevice localDevice, Address from) throws BACnetException {
        BACnetObject obj = localDevice.getObject(objectIdentifier);
        if (obj == null)
            throw createException(ErrorClass.property, ErrorCode.writeAccessDenied, new UnsignedInteger(1));
        
        Encodable e;
        try {
            e = obj.getProperty(propertyIdentifier, propertyArrayIndex);
        }
        catch (BACnetServiceException ex) {
            throw createException(ErrorClass.property, ErrorCode.invalidArrayIndex, new UnsignedInteger(1));
        }
        if (!(e instanceof SequenceOf<?>))
            throw createException(ErrorClass.property, ErrorCode.propertyIsNotAnArray, new UnsignedInteger(1));
        
        SequenceOf<Encodable> propList = (SequenceOf<Encodable>)e;
        
        PropertyValue pv = new PropertyValue(propertyIdentifier, propertyArrayIndex, listOfElements, null);
        if (localDevice.getEventHandler().checkAllowPropertyWrite(obj, pv)) {
            for (int i=1; i<=propList.getCount(); i++) {
                Encodable prop = propList.get(i);
                if (prop != null) {
                    for (Encodable rem : listOfElements) {
                        if (prop.equals(rem))
                            propList.remove(i);
                    }
                }
            }
            localDevice.getEventHandler().propertyWritten(obj, pv);
        }
        else
            throw createException(ErrorClass.property, ErrorCode.writeAccessDenied, new UnsignedInteger(1));
        
        return null;
    }
    
    private BACnetErrorException createException(ErrorClass errorClass, ErrorCode errorCode,
            UnsignedInteger firstFailedElementNumber) {
        return new BACnetErrorException(new ChangeListError(getChoiceId(),
                new BACnetError(errorClass, errorCode), firstFailedElementNumber));
    }
    
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((objectIdentifier == null) ? 0 : objectIdentifier.hashCode());
        result = PRIME * result + ((listOfElements == null) ? 0 : listOfElements.hashCode());
        result = PRIME * result + ((propertyArrayIndex == null) ? 0 : propertyArrayIndex.hashCode());
        result = PRIME * result + ((propertyIdentifier == null) ? 0 : propertyIdentifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final RemoveListElementRequest other = (RemoveListElementRequest) obj;
        if (objectIdentifier == null) {
            if (other.objectIdentifier != null)
                return false;
        }
        else if (!objectIdentifier.equals(other.objectIdentifier))
            return false;
        if (listOfElements == null) {
            if (other.listOfElements != null)
                return false;
        }
        else if (!listOfElements.equals(other.listOfElements))
            return false;
        if (propertyArrayIndex == null) {
            if (other.propertyArrayIndex != null)
                return false;
        }
        else if (!propertyArrayIndex.equals(other.propertyArrayIndex))
            return false;
        if (propertyIdentifier == null) {
            if (other.propertyIdentifier != null)
                return false;
        }
        else if (!propertyIdentifier.equals(other.propertyIdentifier))
            return false;
        return true;
    }
}
