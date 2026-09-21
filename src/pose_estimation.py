def detect_pose(results):

    people_count = 0

    for r in results:

        if r.keypoints is not None:
            people_count += len(r.keypoints)

    annotated_frame = results[0].plot()

    return people_count, annotated_frame