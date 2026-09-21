def analyze_crowd_density(people_count):

    if people_count == 0:
        return "Empty"

    elif people_count <= 2:
        return "Low"

    elif people_count <= 4:
        return "Medium"

    else:
        return "High"