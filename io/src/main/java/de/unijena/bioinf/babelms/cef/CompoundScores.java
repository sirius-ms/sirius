
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
 *         &lt;element ref="{}CpdScore" minOccurs="0"/>
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
    "cpdScore"
})
@XmlRootElement(name = "CompoundScores")
public class CompoundScores {

    @XmlElement(name = "CpdScore")
    protected CpdScore cpdScore;

    /**
     * Gets the value of the cpdScore property.
     * 
     * @return
     *     possible object is
     *     {@link CpdScore }
     *     
     */
    public CpdScore getCpdScore() {
        return cpdScore;
    }

    /**
     * Sets the value of the cpdScore property.
     * 
     * @param value
     *     allowed object is
     *     {@link CpdScore }
     *     
     */
    public void setCpdScore(CpdScore value) {
        this.cpdScore = value;
    }

}
