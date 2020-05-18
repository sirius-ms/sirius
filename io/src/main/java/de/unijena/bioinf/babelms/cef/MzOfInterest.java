
package de.unijena.bioinf.babelms.cef;

import javax.xml.bind.annotation.*;
import java.math.BigDecimal;
import java.math.BigInteger;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}mz"/>
 *       &lt;/sequence>
 *       &lt;attribute name="z" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "mz"
})
@XmlRootElement(name = "MzOfInterest")
public class MzOfInterest {

    @XmlElement(required = true)
    protected BigDecimal mz;
    @XmlAttribute(name = "z")
    protected BigInteger z;

    /**
     * Gets the value of the mz property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getMz() {
        return mz;
    }

    /**
     * Sets the value of the mz property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setMz(BigDecimal value) {
        this.mz = value;
    }

    /**
     * Gets the value of the z property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getZ() {
        return z;
    }

    /**
     * Sets the value of the z property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setZ(BigInteger value) {
        this.z = value;
    }

}
