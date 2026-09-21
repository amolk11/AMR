def robot_navigation_decision(people_count):

    if people_count == 0:
        return "Scanning shelves"

    elif people_count <= 2:
        return "Slowing near customers"

    elif people_count <= 4:
        return "Waiting for clearance"

    else:
        return "Rerouting to another aisle"