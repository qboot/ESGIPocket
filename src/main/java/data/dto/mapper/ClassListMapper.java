package data.dto.mapper;

import data.dto.EClass;
import data.model.Class;
import interfaces.ArrayListMapping;

import java.util.ArrayList;

public class ClassListMapper implements ArrayListMapping<Class, EClass> {

    @Override
    public ArrayList<Class> map(ArrayList<EClass> arrayList) {
        ArrayList<Class> classArrayList = new ArrayList<>();

        for(EClass eClass: arrayList) {
            Class newClass = new Class();
            SpecialityMapper specialityMapper = new SpecialityMapper();
            GroupMapper groupMapper = new GroupMapper();
            YearMapper yearMapper = new YearMapper();
            newClass.setSpeciality(specialityMapper.map(eClass.geteSpeciality()));
            newClass.setGroup(groupMapper.map(eClass.geteGroup()));
            newClass.setYear(yearMapper.map(eClass.geteYear()));
            classArrayList.add(newClass);
        }

        return classArrayList;
    }
}
