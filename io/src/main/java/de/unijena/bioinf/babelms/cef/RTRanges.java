
package de.unijena.bioinf.babelms.cef;

import javax.xml.bind.annotation.*;


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
 *         &lt;element ref="{}RTRange"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "rtRange"
})
@XmlRootElement(name = "RTRanges")
public class RTRanges {

    @XmlElement(name = "RTRange", required = true)
    protected RTRange rtRange;

    /**
     * Gets the value of the rtRange property.
     * 
     * @return
     *     possible object is
     *     {@link RTRange }
     *     
     */
    public RTRange getRTRange() {
        return rtRange;
    }

    /**
     * Sets the value of the rtRange property.
     * 
     * @param value
     *     allowed object is
     *     {@link RTRange }
     *     
     */
    public void setRTRange(RTRange value) {
        this.rtRange = value;
    }

}
